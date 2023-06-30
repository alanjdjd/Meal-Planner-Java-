package mealplanner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.sql.*;
public class appLoop {
    public static void start() throws SQLException {
        boolean flag = true;

        Connection con = database.connectToDatabase();
        database.createTables(con);
        Scanner scanner = new Scanner(System.in);
        while(flag) {
            System.out.println("What would you like to do (add, show, plan, save, exit)?");
            String choice = scanner.nextLine();
            switch (choice) {
                case("add") -> addMeal();
                case("show") -> show();
                case("plan") -> plan();
                case("save") -> save();
                case("exit") -> flag = false;
            }
        }
        System.out.println("Bye!");
        con.close();
    }

    public static void addMeal() throws SQLException {
        Scanner scanner = new Scanner(System.in);
        String meal;
        System.out.println("Which meal do you want to add (breakfast, lunch, dinner)?");
        meal = scanner.nextLine().toLowerCase();
        while(!meal.equals("breakfast") && !meal.equals("lunch") && !meal.equals("dinner") || !verifyFormat(meal)){
            System.out.println("Wrong meal category! Choose from: breakfast, lunch, dinner.");
            meal = scanner.nextLine();
        }
        String mealName;
        System.out.println("Input the meal's name:");
        do {
            mealName = scanner.nextLine();
        }while(!verifyFormat(mealName));

        String ingredients;
        System.out.println("Input the ingredients:");
        do {
            ingredients = scanner.nextLine();
        }while(!verifyFormat(ingredients));

        //INSERTS MEAL CATEGORY AND NAME AND ID
        Connection con = database.connectToDatabase();
        String query = "INSERT INTO meals VALUES(?,?,?)";
        PreparedStatement pstmt = con.prepareStatement(query);

        Statement statementGetId = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        ResultSet RsMealID = statementGetId.executeQuery("SELECT meal_id FROM meals");
        int mealID;
        if(!RsMealID.next()){
            mealID = 1;
        }else{
            RsMealID.last();
            mealID = RsMealID.getInt("meal_id") + 1;
        }

        pstmt.setInt(1, mealID);
        pstmt.setString(2, meal);
        pstmt.setString(3, mealName);
        pstmt.executeUpdate();

        pstmt.close();
        con.close();

        //GET THE BIGGEST MEAL ID
        con = database.connectToDatabase();
        Statement stmt = con.createStatement();
        query = "SELECT meal_id FROM meals";
        ResultSet result = stmt.executeQuery(query);
        int max = Integer.MIN_VALUE;
        while(result.next()){
            max = Math.max(result.getInt("meal_id"), max);
        }

        int IngredientMealID = max;

        result.close();
        stmt.close();
        con.close();

        //INSERTS MEAL INGREDIENTS
        con = database.connectToDatabase();
        String[] ingredientsArray = ingredients.split(",");

        query = "INSERT INTO ingredients (ingredient, meal_id) VALUES(?,?)";
        pstmt = con.prepareStatement(query);
        for (String s : ingredientsArray) {
            pstmt.setString(1, s);
            pstmt.setInt(2, IngredientMealID);
            pstmt.executeUpdate();
        }

        pstmt.close();
        con.close();
        System.out.print("The meal has been added!\n");


    }

    public static boolean verifyFormat(String choice){
        String[] splitChoice = choice.split(",");

        for(int i = 0; i < splitChoice.length; i++){
            splitChoice[i] = splitChoice[i].replace(" ", "");
            String currWord = splitChoice[i];
            if(currWord.equals("")) {
                System.out.println("Wrong format. Use letters only!");
                return false;
            }
            for(int j = 0; j < splitChoice[i].length(); j++){
                if(!Character.isAlphabetic(currWord.charAt(j))){
                    System.out.println("Wrong format. Use letters only!");
                    return false;
                }
            }
        }

        return true;
    }

    public static void show() throws SQLException {
        Scanner scanner = new Scanner(System.in);
        String category;
        System.out.println("Which category do you want to print (breakfast, lunch, dinner)?");
        category = scanner.nextLine();

        while(!Objects.equals(category, "breakfast") &&
                !Objects.equals(category, "lunch") &&
                !Objects.equals(category, "dinner"))
        {
            System.out.println("Wrong meal category! Choose from: breakfast, lunch, dinner.");
            category = scanner.nextLine();
        }

        // Check if table "meals" has given category
        Connection con = database.connectToDatabase();
        String query = "SELECT COUNT(meal) as amount_of_given_category FROM meals WHERE category = ?";
        PreparedStatement countMealsInCategory = con.prepareStatement(query);
        countMealsInCategory.setString(1, category);
        ResultSet rsAmountOfMeals = countMealsInCategory.executeQuery();
        if(rsAmountOfMeals.next()) {
            if(rsAmountOfMeals.getInt(1) == 0) {
                System.out.println("No meals found.");
            } else {
                query = "SELECT meal, meal_id FROM meals WHERE category = ?";
                PreparedStatement pstmtCategoryMeals = con.prepareStatement(query);
                pstmtCategoryMeals.setString(1, category);
                ResultSet rsCategoryMeals = pstmtCategoryMeals.executeQuery();
                System.out.println("Category: " + category);
                while(rsCategoryMeals.next()){
                    System.out.println("Name: " + rsCategoryMeals.getString("meal"));
                    System.out.println("Ingredients: ");
                    int currentMealId = rsCategoryMeals.getInt("meal_id");
                    query = "SELECT ingredient, meal_id from ingredients where meal_id = ?";
                    PreparedStatement pstmtCurrentIngredients = con.prepareStatement(query);
                    pstmtCurrentIngredients.setInt(1, currentMealId);
                    ResultSet rsIngredients = pstmtCurrentIngredients.executeQuery();
                    while(rsIngredients.next()) {
                        System.out.println(rsIngredients.getString("ingredient"));
                    }
                    pstmtCurrentIngredients.close();
                    rsIngredients.close();
                }
                pstmtCategoryMeals.close();
                rsCategoryMeals.close();
            }
        }
        rsAmountOfMeals.close();
        countMealsInCategory.close();
        con.close();
    }

    public static void plan() throws SQLException {
        Connection con = database.connectToDatabase();
        //checks if the table "plan" exists
        PreparedStatement stmtCheckIfPlanExists = con.prepareStatement("SELECT EXISTS ( SELECT 1 FROM pg_tables WHERE tablename = ? )");
        stmtCheckIfPlanExists.setString(1, "plan");
        ResultSet rsBoolean = stmtCheckIfPlanExists.executeQuery();
        rsBoolean.next();
        if(rsBoolean.getBoolean("exists")){
            Statement dropPlan = con.createStatement();
            dropPlan.executeUpdate("DROP TABLE plan");
            dropPlan.close();
        }
        Statement createPlan = con.createStatement();
        createPlan.executeUpdate("CREATE TABLE plan (meal VARCHAR(35), category VARCHAR(35), day VARCHAR(35), meal_id INT )");

        createPlan.close();
        rsBoolean.close();
        stmtCheckIfPlanExists.close();

        //Initialize the creation of plan
        Scanner scanner = new Scanner(System.in);
        String[] daysOfWeekArray = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        String[] categoriesArray = {"breakfast", "lunch", "dinner"};
        for (String day : daysOfWeekArray) {
            System.out.println(day);
            for (String category : categoriesArray) {
                ArrayList<String> mealCategoryList = new ArrayList<>();
                //query selects meal category mealid from table meals where category is equal to current
                String query = "SELECT meal, category, meal_id FROM meals WHERE category = ? ORDER BY meal ASC";
                PreparedStatement pstmtGetCategoryMeals = con.prepareStatement(query);
                pstmtGetCategoryMeals.setString(1, category);
                // resultset get all meals with current category
                ResultSet rsCategoryMeals = pstmtGetCategoryMeals.executeQuery();
                // prints resultset
                while (rsCategoryMeals.next()) {
                    mealCategoryList.add(rsCategoryMeals.getString("meal"));
                    System.out.println(rsCategoryMeals.getString("meal"));
                }
                String choice;
                System.out.printf("Choose the %s for %s from the list above:\n", category, day);
                choice = scanner.nextLine();
                boolean isCorrect = false;
                while(!isCorrect){
                    for (String s : mealCategoryList) {
                        if (Objects.equals(s, choice)) {
                            isCorrect = true;
                            break;
                        }
                    }
                    if(!isCorrect){
                        System.out.println("This meal doesn’t exist. Choose a meal from the list above.");
                        choice = scanner.nextLine();
                    }
                }

                pstmtGetCategoryMeals.close();
                rsCategoryMeals.close();

                //add choice as a new meal into plean with the same id as meal in table meals
                //get choice id
                int choiceID = 0;
                String pstmtQuery = "SELECT meal, meal_id FROM meals WHERE meal = ?";
                PreparedStatement pstmtGetID = con.prepareStatement(pstmtQuery);
                pstmtGetID.setString(1, choice);
                ResultSet rsChoiceID = pstmtGetID.executeQuery();
                if(rsChoiceID.next()){
                    choiceID = rsChoiceID.getInt("meal_id");
                }

                pstmtGetID.close();
                rsChoiceID.close();

                //insert choice and choiceid and category into plan table
                //choice, choiceID, category, day
                String queryInsertChoice = "INSERT INTO plan VALUES(?,?,?,?)";
                PreparedStatement pstmtInsertChoice = con.prepareStatement(queryInsertChoice);
                pstmtInsertChoice.setString(1, choice);
                pstmtInsertChoice.setString(2, category);
                pstmtInsertChoice.setString(3, day);
                pstmtInsertChoice.setInt(4, choiceID);
                pstmtInsertChoice.executeUpdate();
                pstmtInsertChoice.close();
            }
            System.out.printf("Yeah! We planned the meals for %s.\n\n", day);

        }
        con.close();
        showPlan();
    }
    public static void showPlan() throws SQLException {
        Connection con = database.connectToDatabase();
        String[] daysOfWeekArray = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        PreparedStatement pstmt = con.prepareStatement("Select * from plan where day = ?");
        for(String day : daysOfWeekArray){
            pstmt.setString(1, day);
            ResultSet rsMeals = pstmt.executeQuery();
            System.out.println(day);
            rsMeals.next();
            System.out.printf("Breakfast: %s\n", rsMeals.getString("meal"));
            rsMeals.next();
            System.out.printf("Lunch: %s\n", rsMeals.getString("meal"));
            rsMeals.next();
            System.out.printf("Dinner: %s\n", rsMeals.getString("meal"));
        }

    }
    public static void save() throws SQLException {
        Connection con = database.connectToDatabase();
        //checks if the table "plan" exists
        Statement stmtCheckIfPlanExists = con.createStatement();
        ResultSet rsBoolean = stmtCheckIfPlanExists.executeQuery("SELECT EXISTS ( SELECT 1 FROM pg_tables WHERE tablename = 'plan')");
        rsBoolean.next();
        if(!rsBoolean.getBoolean("exists")){
            System.out.println("Unable to save. Plan your meals first.");
        }else{
            System.out.println("Input a filename:");
            Scanner scanner = new Scanner(System.in);
            String filename = scanner.nextLine();
            createIngredientList(filename);
        }
    }

    public static void createIngredientList(String filename) throws SQLException {
        // Gets meal_id of meals in the plan
        Connection con = database.connectToDatabase();
        Statement stmtGetMealsID = con.createStatement();
        String queryGetMealsID = "SELECT meal_id FROM plan";
        ResultSet rsMealsID = stmtGetMealsID.executeQuery(queryGetMealsID);
        ArrayList<Integer> ListMealID = new ArrayList<>();
        while(rsMealsID.next()){
            ListMealID.add(rsMealsID.getInt("meal_id"));
        }

        stmtGetMealsID.close();
        rsMealsID.close();

        // Gets ingredients from meals with the same id
        HashMap<String, Integer> mapIngredients = new HashMap<>();
        ArrayList<String> currentIngredients = new ArrayList<>();
        for(int currentID : ListMealID) {
            // Save ingredients into arraylist "currentIngredients"
            String queryGetIngredients = "select ingredient from ingredients where meal_id = ?";
            PreparedStatement pstmtGetIngredients = con.prepareStatement(queryGetIngredients);
            pstmtGetIngredients.setInt(1, currentID);
            ResultSet rsIngredients = pstmtGetIngredients.executeQuery();
            while (rsIngredients.next()) {
                currentIngredients.add(rsIngredients.getString("ingredient"));
            }
        }
            // Add ingredients to map
            for (String ingredient : currentIngredients) {
                int amount = Collections.frequency(currentIngredients, ingredient);
                mapIngredients.put(ingredient, amount);
            }
        // Create file
        try{
            File ingredientList = new File(filename);
            if(ingredientList.createNewFile()){
                System.out.println("Saved!");
                FileWriter myWriter = new FileWriter(ingredientList);
                for(String key : mapIngredients.keySet()){
                    if(mapIngredients.get(key) == 1) {
                        myWriter.write(key + "\n");
                    }else{
                        myWriter.write(key + " x" + mapIngredients.get(key) + "\n");
                    }
                }
                myWriter.close();
            }else {
                System.out.println("File already exists!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
