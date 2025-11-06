
import pandas as pd
import numpy as np
from sklearn.compose import ColumnTransformer
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler, OneHotEncoder
from sklearn.impute import SimpleImputer
from sklearn.cluster import KMeans
import os

# The user wants to calculate the numeric distances between the inertia for every K in a graph.
# The user has provided a jupyter notebook file: "Quiz 1 - part 2 .ipynb"
# I will first read the csv file into a pandas dataframe
df = pd.read_csv(os.path.join("/Users/tvo/Documents/portfolio/college-notes/CSC 180", "Data06JoMix.csv"))

# Define features
numeric_features = ['Age', 'Height_cm', 'Weight_kg']
categorical_features = ['Sex', 'Country']

numeric_transformer = Pipeline(steps=[
    ('imputer', SimpleImputer(strategy='median')),
    ('scaler', StandardScaler())
])

categorical_transformer = Pipeline(steps=[
    ('onehot', OneHotEncoder(handle_unknown='ignore'))
])

preprocessor = ColumnTransformer(
    transformers=[
        ('num', numeric_transformer, numeric_features),
        ('cat', categorical_transformer, categorical_features)],
    remainder='drop'
)

preprocessed_data = preprocessor.fit_transform(df)

# Calculate inertia for a range of K values
inertia = []
K = range(1, 11)
for k in K:
    kmeans = KMeans(n_clusters=k, random_state=42, n_init=10)
    kmeans.fit(preprocessed_data)
    inertia.append(kmeans.inertia_)

# Calculate the difference in inertia between consecutive K values
inertia_distances = np.diff(inertia)

# Print the distances
print("Inertia distances:")
for i, dist in enumerate(inertia_distances):
    print(f"K={i+1} to K={i+2}: {dist}")
