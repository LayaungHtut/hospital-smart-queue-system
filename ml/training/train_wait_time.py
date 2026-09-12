#!/usr/bin/env python3
"""
Predictive Wait Time Model Training
Trains XGBoost/LightGBM model to predict actual wait times per doctor/department/time-of-day
Exports to ONNX for Java inference
"""

import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder, StandardScaler
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
import xgboost as xgb
import lightgbm as lgb
import joblib
import json
from datetime import datetime
import warnings
warnings.filterwarnings('ignore')

# Try to import onnx conversion
try:
    from skl2onnx import convert_sklearn
    from skl2onnx.common.data_types import FloatTensorType
    ONNX_AVAILABLE = True
except ImportError:
    ONNX_AVAILABLE = False
    print("ONNX conversion not available. Install skl2onnx: pip install skl2onnx onnxmltools")


def generate_synthetic_data(n_samples=50000):
    """Generate synthetic historical queue data for training"""
    np.random.seed(42)
    
    departments = ['CAR', 'NEU', 'ORT', 'GEN', 'PED', 'DER']
    doctors_per_dept = {
        'CAR': ['D001', 'D002', 'D003'],
        'NEU': ['D004', 'D005'],
        'ORT': ['D006', 'D007', 'D008'],
        'GEN': ['D009', 'D010', 'D011', 'D012'],
        'PED': ['D013', 'D014'],
        'DER': ['D015', 'D016']
    }
    
    data = []
    for _ in range(n_samples):
        dept = np.random.choice(departments)
        doctor = np.random.choice(doctors_per_dept[dept])
        
        # Time features
        hour = np.random.randint(8, 20)
        day_of_week = np.random.randint(0, 7)  # 0=Monday
        month = np.random.randint(1, 13)
        is_weekend = 1 if day_of_week >= 5 else 0
        is_peak_hour = 1 if hour in [9, 10, 11, 14, 15, 16] else 0
        
        # Queue state features
        queue_length = np.random.poisson(8)  # patients waiting
        avg_consultation = np.random.normal(15, 3)  # doctor's avg consultation time
        doctor_experience = np.random.uniform(1, 30)  # years
        doctor_speed_factor = np.random.normal(1.0, 0.15)  # relative speed
        
        # Patient features
        patient_age = np.random.randint(1, 90)
        patient_priority = np.random.choice([1, 2, 3], p=[0.1, 0.3, 0.6])  # 1=Emergency, 2=Appointment, 3=Normal
        is_new_patient = np.random.choice([0, 1], p=[0.7, 0.3])
        
        # Historical features (rolling averages)
        dept_avg_wait_last_hour = np.random.normal(25, 10)
        doctor_avg_wait_last_hour = np.random.normal(20, 8)
        dept_queue_trend = np.random.normal(0, 2)  # increasing/decreasing
        
        # Target: actual wait time in minutes
        # Base wait = queue_length * avg_consultation / doctor_speed
        base_wait = queue_length * avg_consultation / doctor_speed_factor
        
        # Adjustments
        wait_time = base_wait
        wait_time *= (1 + 0.3 * is_peak_hour)
        wait_time *= (1 + 0.2 * is_weekend)
        wait_time *= (1 - 0.1 * (doctor_experience / 30))  # experienced doctors faster
        wait_time *= (1 + 0.15 * (patient_priority == 3))  # normal patients wait longer
        wait_time += np.random.normal(0, 5)  # noise
        
        wait_time = max(0, wait_time)  # no negative wait times
        
        data.append({
            'department': dept,
            'doctor_id': doctor,
            'hour': hour,
            'day_of_week': day_of_week,
            'month': month,
            'is_weekend': is_weekend,
            'is_peak_hour': is_peak_hour,
            'queue_length': queue_length,
            'avg_consultation_minutes': avg_consultation,
            'doctor_experience_years': doctor_experience,
            'doctor_speed_factor': doctor_speed_factor,
            'patient_age': patient_age,
            'patient_priority': patient_priority,
            'is_new_patient': is_new_patient,
            'dept_avg_wait_last_hour': dept_avg_wait_last_hour,
            'doctor_avg_wait_last_hour': doctor_avg_wait_last_hour,
            'dept_queue_trend': dept_queue_trend,
            'actual_wait_time': wait_time
        })
    
    return pd.DataFrame(data)


def prepare_features(df):
    """Prepare features for training"""
    # Encode categorical variables
    le_dept = LabelEncoder()
    le_doctor = LabelEncoder()
    
    df['department_encoded'] = le_dept.fit_transform(df['department'])
    df['doctor_encoded'] = le_doctor.fit_transform(df['doctor_id'])
    
    # Cyclical encoding for time
    df['hour_sin'] = np.sin(2 * np.pi * df['hour'] / 24)
    df['hour_cos'] = np.cos(2 * np.pi * df['hour'] / 24)
    df['day_sin'] = np.sin(2 * np.pi * df['day_of_week'] / 7)
    df['day_cos'] = np.cos(2 * np.pi * df['day_of_week'] / 7)
    df['month_sin'] = np.sin(2 * np.pi * df['month'] / 12)
    df['month_cos'] = np.cos(2 * np.pi * df['month'] / 12)
    
    # Interaction features
    df['queue_x_consultation'] = df['queue_length'] * df['avg_consultation_minutes']
    df['queue_x_speed'] = df['queue_length'] * df['doctor_speed_factor']
    
    feature_cols = [
        'department_encoded', 'doctor_encoded',
        'hour_sin', 'hour_cos', 'day_sin', 'day_cos', 'month_sin', 'month_cos',
        'is_weekend', 'is_peak_hour',
        'queue_length', 'avg_consultation_minutes', 'doctor_experience_years',
        'doctor_speed_factor', 'patient_age', 'patient_priority', 'is_new_patient',
        'dept_avg_wait_last_hour', 'doctor_avg_wait_last_hour', 'dept_queue_trend',
        'queue_x_consultation', 'queue_x_speed'
    ]
    
    X = df[feature_cols].values
    y = df['actual_wait_time'].values
    
    return X, y, feature_cols, le_dept, le_doctor


def train_xgboost(X_train, y_train, X_val, y_val):
    """Train XGBoost model"""
    dtrain = xgb.DMatrix(X_train, label=y_train)
    dval = xgb.DMatrix(X_val, label=y_val)
    
    params = {
        'objective': 'reg:squarederror',
        'eval_metric': 'rmse',
        'max_depth': 6,
        'learning_rate': 0.1,
        'subsample': 0.8,
        'colsample_bytree': 0.8,
        'min_child_weight': 3,
        'reg_alpha': 0.1,
        'reg_lambda': 1.0,
        'seed': 42
    }
    
    model = xgb.train(
        params,
        dtrain,
        num_boost_round=500,
        evals=[(dtrain, 'train'), (dval, 'val')],
        early_stopping_rounds=50,
        verbose_eval=50
    )
    return model


def train_lightgbm(X_train, y_train, X_val, y_val):
    """Train LightGBM model"""
    train_data = lgb.Dataset(X_train, label=y_train)
    val_data = lgb.Dataset(X_val, label=y_val, reference=train_data)
    
    params = {
        'objective': 'regression',
        'metric': 'rmse',
        'boosting_type': 'gbdt',
        'num_leaves': 31,
        'learning_rate': 0.1,
        'feature_fraction': 0.8,
        'bagging_fraction': 0.8,
        'bagging_freq': 5,
        'verbose': -1,
        'seed': 42
    }
    
    model = lgb.train(
        params,
        train_data,
        num_boost_round=500,
        valid_sets=[train_data, val_data],
        valid_names=['train', 'val'],
        callbacks=[lgb.early_stopping(50), lgb.log_evaluation(50)]
    )
    return model


def evaluate_model(model, X_test, y_test, model_name, feature_cols):
    """Evaluate model performance"""
    if hasattr(model, 'predict'):
        if 'xgb' in model_name.lower():
            import xgboost as xgb
            dtest = xgb.DMatrix(X_test)
            y_pred = model.predict(dtest)
        else:
            y_pred = model.predict(X_test)
    else:
        y_pred = model.predict(X_test)
    
    mae = mean_absolute_error(y_test, y_pred)
    rmse = np.sqrt(mean_squared_error(y_test, y_pred))
    r2 = r2_score(y_test, y_pred)
    
    print(f"\n{model_name} Performance:")
    print(f"  MAE:  {mae:.2f} minutes")
    print(f"  RMSE: {rmse:.2f} minutes")
    print(f"  R²:   {r2:.4f}")
    
    # Feature importance
    if hasattr(model, 'feature_importances_'):
        importance = model.feature_importances_
    elif hasattr(model, 'get_score'):
        importance_dict = model.get_score(importance_type='gain')
        importance = np.array([importance_dict.get(f'f{i}', 0) for i in range(len(feature_cols))])
    else:
        importance = None
    
    if importance is not None:
        top_features = sorted(zip(feature_cols, importance), key=lambda x: x[1], reverse=True)[:10]
        print(f"  Top 10 features:")
        for feat, imp in top_features:
            print(f"    {feat}: {imp:.4f}")
    
    return {'mae': mae, 'rmse': rmse, 'r2': r2, 'predictions': y_pred}


def export_to_onnx(model, feature_cols, model_name, output_dir='models'):
    """Export model to ONNX format"""
    if not ONNX_AVAILABLE:
        print(f"Skipping ONNX export for {model_name} (skl2onnx not installed)")
        return
    
    try:
        # For tree-based models, we need to wrap in a sklearn-compatible estimator
        # This is a simplified approach - in production use onnxmltools for XGBoost/LightGBM
        from sklearn.ensemble import RandomForestRegressor
        from sklearn.pipeline import Pipeline
        
        print(f"ONNX export for {model_name} requires onnxmltools. Skipping for now.")
        print(f"Install: pip install onnxmltools")
    except Exception as e:
        print(f"ONNX export failed for {model_name}: {e}")


def save_model_artifacts(model, feature_cols, le_dept, le_doctor, scaler, model_name, output_dir='models'):
    """Save model and preprocessing artifacts"""
    import os
    os.makedirs(output_dir, exist_ok=True)
    
    # Save model
    if 'xgb' in model_name.lower():
        model.save_model(f'{output_dir}/{model_name}.json')
    else:
        joblib.dump(model, f'{output_dir}/{model_name}.pkl')
    
    # Save preprocessing artifacts
    artifacts = {
        'feature_columns': feature_cols,
        'dept_classes': le_dept.classes_.tolist(),
        'doctor_classes': le_doctor.classes_.tolist(),
        'scaler_mean': scaler.mean_.tolist() if scaler else None,
        'scaler_scale': scaler.scale_.tolist() if scaler else None,
        'model_type': model_name,
        'created_at': datetime.now().isoformat()
    }
    
    with open(f'{output_dir}/{model_name}_artifacts.json', 'w') as f:
        json.dump(artifacts, f, indent=2)
    
    print(f"Saved {model_name} artifacts to {output_dir}/")


def main():
    print("=" * 60)
    print("Predictive Wait Time Model Training")
    print("=" * 60)
    
    # Generate data
    print("\nGenerating synthetic training data...")
    df = generate_synthetic_data(50000)
    print(f"Generated {len(df)} samples")
    
    # Prepare features
    print("\nPreparing features...")
    X, y, feature_cols, le_dept, le_doctor = prepare_features(df)
    
    # Scale features
    scaler = StandardScaler()
    X_scaled = scaler.fit_transform(X)
    
    # Split data
    X_train, X_temp, y_train, y_temp = train_test_split(X_scaled, y, test_size=0.3, random_state=42)
    X_val, X_test, y_val, y_test = train_test_split(X_temp, y_temp, test_size=0.5, random_state=42)
    
    print(f"Train: {len(X_train)}, Val: {len(X_val)}, Test: {len(X_test)}")
    
    # Train XGBoost
    print("\n" + "=" * 60)
    print("Training XGBoost...")
    print("=" * 60)
    xgb_model = train_xgboost(X_train, y_train, X_val, y_val)
    xgb_metrics = evaluate_model(xgb_model, X_test, y_test, 'XGBoost', feature_cols)
    save_model_artifacts(xgb_model, feature_cols, le_dept, le_doctor, scaler, 'wait_time_xgboost')
    
    # Train LightGBM
    print("\n" + "=" * 60)
    print("Training LightGBM...")
    print("=" * 60)
    lgb_model = train_lightgbm(X_train, y_train, X_val, y_val)
    lgb_metrics = evaluate_model(lgb_model, X_test, y_test, 'LightGBM', feature_cols)
    save_model_artifacts(lgb_model, feature_cols, le_dept, le_doctor, scaler, 'wait_time_lightgbm')
    
    # Choose best model
    best_model_name = 'XGBoost' if xgb_metrics['mae'] < lgb_metrics['mae'] else 'LightGBM'
    print(f"\nBest model: {best_model_name} (MAE: {min(xgb_metrics['mae'], lgb_metrics['mae']):.2f})")
    
    # Save best model info
    with open('models/best_model.txt', 'w') as f:
        f.write(best_model_name.lower().replace('xgboost', 'xgboost').replace('lightgbm', 'lightgbm'))
    
    print("\nTraining complete!")


if __name__ == '__main__':
    main()