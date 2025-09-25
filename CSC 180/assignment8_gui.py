import tkinter as tk
from tkinter import ttk
import pandas as pd
from sklearn.linear_model import LinearRegression
from sklearn.model_selection import train_test_split

# --- Model Training Code from Notebook ---

# 1. Load the data from the CSV file into a pandas DataFrame
csv_file_path = '/Users/tvo/Library/CloudStorage/OneDrive-CaliforniaStateUniversity,Sacramento/college-notes/CSC 180/carprices4.csv'
df = pd.read_csv(csv_file_path)

# --- Dummy Model Training ---
# 2. Dummy Encode the Car Types
dummies = pd.get_dummies(df["Car Model"], prefix='Car_Model', drop_first=True)
df_processed = pd.concat([df.drop('Car Model', axis='columns'), dummies], axis='columns')

# 3. Create and training and test data - DUMMY
target = 'Sell Price($)'
features = [col for col in df_processed.columns if col != target]

X = df_processed[features]
y = df_processed[target]

# We train on the whole dataset for the GUI application
model = LinearRegression()
model.fit(X, y)


# --- OHE Model Training ---
# 2. OHE Encode the Car Types
dummies_ohe = pd.get_dummies(df["Car Model"], prefix='Car_Model')
df_ohe = pd.concat([df.drop('Car Model', axis='columns'), dummies_ohe], axis='columns')

# 3. Create and training data - OHE
features_ohe = [col for col in df_ohe.columns if col != target]
X_ohe = df_ohe[features_ohe]
y_ohe = df_ohe[target]

model_ohe = LinearRegression()
model_ohe.fit(X_ohe, y_ohe)


# --- GUI Code ---

def calculate_price():
    try:
        company = car_model.get()
        age = int(car_age.get())
        mileage = int(car_mileage.get())
        prediction_models = prediction_model.get()
        
        models = {
            "Linear Regression w/ Dummy": model,
            "Linear Regression w/ 1-hot encoding": model_ohe
        }

        selected_model = models.get(prediction_models)
        
        if not selected_model:
            result_label.config(text="Please select a model.")
            return
            
        if not company:
            result_label.config(text="Please select a car model.")
            return

        features = []
        if prediction_models == "Linear Regression w/ Dummy":
            # The model was trained on features in this order:
            # ['Mileage', 'Age(yrs)', 'Car_Model_Audi A5', 'Car_Model_BMW X5']
            is_audi = 1 if company == "Audi" else 0
            is_bmw = 1 if company == "BMW" else 0
            features = [mileage, age, is_audi, is_bmw]
        elif prediction_models == "Linear Regression w/ 1-hot encoding":
            # We assume the OHE model was trained on all car models,
            # and the order of features is mileage, age, and then the car models
            # alphabetically: Audi, BMW, Mercedes.
            is_audi = 1 if company == "Audi" else 0
            is_bmw = 1 if company == "BMW" else 0
            is_mercedes = 1 if company == "Mercedes" else 0
            features = [mileage, age, is_audi, is_bmw, is_mercedes]

        if features:
            price = selected_model.predict([features])[0]
            result_label.config(text=f'Estimated Price: ${price:,.2f}')
        else:
            result_label.config(text=f'Please select a valid model.')

    except ValueError:
        result_label.config(text=f'Please Enter Valid Numbers for age and mileage')
    except Exception as e:
        result_label.config(text=f'An error occurred: {e}')


def reset_fields():
    car_model.set("")
    car_age.delete(0, tk.END)
    car_mileage.delete(0, tk.END)
    prediction_model.set("")
    result_label.config(text="Estimated Price")

#GUI
root = tk.Tk()
root.title("Assignment 8")
root.geometry("400x500")

# Create a main frame
main_frame = tk.Frame(root, padx=10, pady=10)
main_frame.pack(fill=tk.BOTH, expand=True)

#Prediction Model
tk.Label(main_frame, text="Prediction Model: ").grid(row=0, column=0, sticky="w", pady=5)
prediction_model = ttk.Combobox(main_frame, values=["Linear Regression w/ Dummy", "Linear Regression w/ 1-hot encoding"])
prediction_model.grid(row=0, column=1, sticky="ew", pady=5)


#Car Model
tk.Label(main_frame, text="Car Model: ").grid(row=1, column=0, sticky="w", pady=5)
car_model = ttk.Combobox(main_frame, values=["BMW", "Audi", "Mercedes"])
car_model.grid(row=1, column=1, sticky="ew", pady=5)
#Age
tk.Label(main_frame, text="Age (yrs): ").grid(row=2, column=0, sticky="w", pady=5)
car_age = ttk.Entry(main_frame)
car_age.grid(row=2, column=1, sticky="ew", pady=5)

#Mileage
tk.Label(main_frame, text="Mileage (km): ").grid(row=3, column=0, sticky="w", pady=5)
car_mileage = ttk.Entry(main_frame)
car_mileage.grid(row=3, column=1, sticky="ew", pady=5)

#Buttons
button_frame = tk.Frame(main_frame)
button_frame.grid(row=4, column=0, columnspan=2, pady=10)

calc_button = tk.Button(button_frame, text="Calculate Price", command=calculate_price)
calc_button.grid(row=0, column=0, padx=5)

reset_button = tk.Button(button_frame, text="Reset", command=reset_fields)
reset_button.grid(row=0, column=1, padx=5)

#Result
result_label = tk.Label(main_frame, text="Estimated Price: ")
result_label.grid(row=5, column=0, columnspan=2, pady=10)

main_frame.columnconfigure(1, weight=1)

root.mainloop()