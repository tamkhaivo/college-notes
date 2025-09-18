import pandas as pd
from sklearn.linear_model import LinearRegression
import math
import argparse

def train_home_price_model(file_path):
    """
    Loads home price data, trains a linear regression model, and prints the results.
    Handles different data schemas by detecting column names.
 
    Args:
        file_path (str): The path to the CSV file.
    """
    try:
        # 1. Load the data from the CSV file into a pandas DataFrame
        df = pd.read_csv(file_path)
        print("Original Data:")
        print(df)
        print("-" * 30)
    except FileNotFoundError:
        print(f"Error: The file at {file_path} was not found.")
        return
 
    # --- Model Training Logic ---
    model = LinearRegression()
    
    # Check which schema the CSV file follows and process accordingly
    if 'town' in df.columns:
        # Schema with categorical 'town' feature (e.g., homeprices3.csv)
        print("\nProcessing data with 'town' as a categorical feature using one-hot encoding.")
 
        # 2. Create dummy variables for the 'town' column
        dummies = pd.get_dummies(df.town, prefix='town')
        # Merge dummies with the original DataFrame
        df_processed = pd.concat([df.drop('town', axis='columns'), dummies], axis='columns')
 
        # To avoid the dummy variable trap, we drop one of the dummy columns.
        # The model learns this as the 'base case'.
        # We will drop the last one alphabetically ('town_west windsor')
        df_final = df_processed.drop('town_west windsor', axis='columns')
 
        print("\nData after one-hot encoding (one town column dropped):")
        print(df_final)
        print("-" * 30)
 
        # 3. Separate features (X) and the target variable (y)
        features = [col for col in df_final.columns if col != 'price']
        target = 'price'
        X = df_final[features]
        y = df_final[target]
 
    elif all(col in df.columns for col in ['area', 'bedrooms', 'age']):
        # Schema with numerical features (e.g., homePricesMedian.csv)
        print("\nProcessing data with 'area', 'bedrooms', 'age' features.")
 
        # 2. Handle missing data
        median_bedrooms = math.floor(df.bedrooms.median())
        df.bedrooms = df.bedrooms.fillna(median_bedrooms)
        print("\nData after handling missing values:")
        print(df)
        print("-" * 30)
 
        # 3. Separate features (X) and the target variable (y)
        features = ['area', 'bedrooms', 'age']
        target = 'price'
        X = df[features]
        y = df[target]
    else:
        print("\nError: CSV file does not match a known format.")
        print("Expected columns: ('town', 'area', 'price') OR ('area', 'bedrooms', 'age', 'price')")
        return
 
    # 4. Create and train the Linear Regression model
    model.fit(X, y)
 
    # 5. Display the results of the trained model
    print("\nModel Coefficients:")
    for feature, coef in zip(features, model.coef_):
        print(f"  - {feature}: {coef:.2f}")
 
    print(f"\nModel Intercept: {model.intercept_:.2f}")
    print("-" * 30)
 
    # 6. Use the model to make predictions
    print("\nExample Predictions:")
    if 'town_monroe township' in features:
        # Predictions for the 'town' schema
        # Input format: [area, town_monroe township, town_robinsville]
        price_monroe = model.predict([[3400, 1, 0]])
        print(f"  - Predicted price for a 3400 sq ft house in Monroe Township: ${price_monroe[0]:,.2f}")
        
        price_robbinsville = model.predict([[2800, 0, 1]])
        print(f"  - Predicted price for a 2800 sq ft house in Robbinsville: ${price_robbinsville[0]:,.2f}")

        price_west_windsor = model.predict([[3100, 0, 0]]) # Base case
        print(f"  - Predicted price for a 3100 sq ft house in West Windsor: ${price_west_windsor[0]:,.2f}")
    else:
        # Predictions for the 'bedrooms'/'age' schema
        predicted_price_1 = model.predict([[3000, 3, 10]])
        print(f"  - Predicted price for a 3000 sq ft, 3 bedroom, 10 year old house: ${predicted_price_1[0]:,.2f}")
        predicted_price_2 = model.predict([[2500, 4, 5]])
        print(f"  - Predicted price for a 2500 sq ft, 4 bedroom, 5 year old house: ${predicted_price_2[0]:,.2f}")

if __name__ == "__main__":
    # Set up the command-line argument parser
    parser = argparse.ArgumentParser(
        description="Train a linear regression model on home price data from a CSV file."
    )
    parser.add_argument(
        "file_path", help="Path to the CSV file containing home price data."
    )
    args = parser.parse_args()

    # Run the training function with the provided file path
    train_home_price_model(args.file_path)
