"""
Trains the linear regression used by CalorieBurnModel.kt
(app/src/main/java/com/example/glarmto/data/util/CalorieBurnModel.kt).

Dataset: "Calories Burnt Prediction" (15,000 rows: Gender, Age, Height, Weight,
Duration, Heart_Rate, Body_Temp -> Calories). Originally a Kaggle dataset; mirrored at
https://raw.githubusercontent.com/mdatheeb/Calories-Burned-Predictor/main/Linear/calories.csv

Heart_Rate and Body_Temp are dropped: GlarmTo has no sensor for either, and faking
them at inference time would make the estimate dishonest. The remaining 5 features
(Gender, Age, Height, Weight, Duration) are all things the app genuinely has, from
the user's profile plus a logged workout session's duration.

Usage:
    pip install numpy pandas
    python train_calorie_model.py
"""

import numpy as np
import pandas as pd

CSV_PATH = "calories.csv"
FEATURES = ["Gender", "Age", "Height", "Weight", "Duration"]


def fit_ols(x: np.ndarray, y: np.ndarray) -> np.ndarray:
    x_with_bias = np.hstack([np.ones((x.shape[0], 1)), x])
    coef, *_ = np.linalg.lstsq(x_with_bias, y, rcond=None)
    return coef


def score(coef: np.ndarray, x: np.ndarray, y: np.ndarray) -> tuple[float, float]:
    pred = np.hstack([np.ones((x.shape[0], 1)), x]) @ coef
    ss_res = np.sum((y - pred) ** 2)
    ss_tot = np.sum((y - np.mean(y)) ** 2)
    r2 = 1 - ss_res / ss_tot
    rmse = float(np.sqrt(np.mean((y - pred) ** 2)))
    return float(r2), rmse


def main() -> None:
    df = pd.read_csv(CSV_PATH)
    df["Gender"] = (df["Gender"] == "male").astype(int)

    x = df[FEATURES].values.astype(float)
    y = df["Calories"].values.astype(float)

    # 80/20 holdout to report an honest, unseen-data score before shipping.
    rng = np.random.default_rng(42)
    idx = rng.permutation(len(df))
    split = int(len(df) * 0.8)
    train_idx, test_idx = idx[:split], idx[split:]

    holdout_coef = fit_ols(x[train_idx], y[train_idx])
    r2_test, rmse_test = score(holdout_coef, x[test_idx], y[test_idx])
    print(f"Holdout test  R^2={r2_test:.4f}  RMSE={rmse_test:.2f} kcal")

    # Ship the version trained on all rows.
    final_coef = fit_ols(x, y)
    r2_full, rmse_full = score(final_coef, x, y)
    print(f"Full-data fit R^2={r2_full:.4f}  RMSE={rmse_full:.2f} kcal")

    print("\nCoefficients to paste into CalorieBurnModel.kt:")
    names = ["INTERCEPT", "COEF_MALE", "COEF_AGE", "COEF_HEIGHT_CM", "COEF_WEIGHT_KG", "COEF_DURATION_MIN"]
    for name, value in zip(names, final_coef):
        print(f"    private const val {name} = {value}")


if __name__ == "__main__":
    main()
