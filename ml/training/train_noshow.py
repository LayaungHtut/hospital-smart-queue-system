#!/usr/bin/env python3
"""
No-Show Prediction Model Training
Binary classifier to predict patient no-show probability
Exports to ONNX for Java inference
"""

import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder, StandardScaler
from sklearn.metrics import (roc_auc_score, accuracy_score, precision_score, 
                             recall_score, f1_score, confusion_matrix, classification_report)
import xgboost as xgb
import lightgbm as lgb
import joblib
import json
from datetime import datetime, timedelta
import warnings
warnings.filterwarnings('ignore')


def generate_synthetic_noshow_data(n_samples=100000):
    """Generate synthetic historical appointment data for no-show prediction"""
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
    base_date = datetime(2024, 1, 1)
    
    for _ in range(n_samples):
        dept = np.random.choice(departments)
        doctor = np.random.choice(doctors_per_dept[dept])
        
        # Appointment timing
        days_ahead = np.random.randint(0, 30)
        appt_date = base_date + timedelta(days=days_ahead)
        hour = np.random.randint(8, 18)
        day_of_week = appt_date.weekday()
        month = appt_date.month
        is_weekend = 1 if day_of_week >= 5 else 0
        
        # Patient demographics
        patient_age = np.random.randint(1, 95)
        patient_gender = np.random.choice(['M', 'F'])
        patient_distance_km = np.random.exponential(15)  # distance from hospital
        insurance_type = np.random.choice(['public', 'private', 'self_pay'], p=[0.6, 0.3, 0.1])
        
        # Patient history
        total_past_appointments = np.random.poisson(5)
        past_noshows = np.random.binomial(total_past_appointments, 0.15) if total_past_appointments > 0 else 0
        past_cancellations = np.random.binomial(total_past_appointments, 0.1) if total_past_appointments > 0 else 0
        noshow_rate = past_noshows / total_past_appointments if total_past_appointments > 0 else 0
        
        days_since_last_visit = np.random.exponential(60) if total_past_appointments > 0 else 365
        avg_lead_time = np.random.exponential(14)  # average days they book ahead
        
        # Appointment characteristics
        is_followup = np.random.choice([0, 1], p=[0.4, 0.6])
        priority = np.random.choice([1, 2, 3], p=[0.05, 0.25, 0.7])  # 1=Emergency, 2=Urgent, 3=Routine
        appointment_type = np.random.choice(['consultation', 'procedure', 'checkup'], p=[0.5, 0.2, 0.3])
        
        # Weather (simplified)
        temperature = np.random.normal(22, 8)
        precipitation = np.random.exponential(2)
        is_bad_weather = 1 if precipitation > 5 or temperature < 5 or temperature > 35 else 0
        
        # Communication
        received_reminder = np.random.choice([0, 1], p=[0.15, 0.85])
        reminder_channel = np.random.choice(['sms', 'email', 'call', 'none'], p=[0.5, 0.2, 0.15, 0.15])
        confirmed_appointment = np.random.choice([0, 1], p=[0.3, 0.7]) if received_reminder else 0
        
        # Target: no-show (1) or show (0)
        # Base no-show rate ~15%
        logit = -2.0  # base log-odds
        
        # Patient history effects
        logit += 2.5 * noshow_rate  # strong predictor
        logit += 0.5 * (past_cancellations / max(total_past_appointments, 1))
        logit += -0.01 * days_since_last_visit  # recent patients more likely to show
        
        # Demographics
        logit += -0.02 * patient_age  # older patients more reliable
        logit += 0.3 * (patient_gender == 'M')  # slight gender effect
        logit += 0.01 * patient_distance_km  # farther = more no-shows
        logit += 0.4 * (insurance_type == 'self_pay')  # self-pay more no-shows
        logit += -0.3 * (insurance_type == 'private')
        
        # Appointment characteristics
        logit += -0.8 * (priority == 1)  # emergency rarely no-show
        logit += -0.4 * (priority == 2)
        logit += 0.3 * is_followup  # followups more no-shows
        logit += 0.2 * (appointment_type == 'procedure')
        
        # Timing
        logit += 0.02 * days_ahead  # further ahead = more no-shows
        logit += 0.2 * is_weekend
        logit += -0.1 * (hour < 10)  # early morning more reliable
        logit += 0.3 * is_bad_weather
        
        # Communication
        logit += -0.6 * received_reminder
        logit += -0.4 * confirmed_appointment
        logit += 0.2 * (reminder_channel == 'none')
        
        # Doctor effect (some doctors have more no-shows)
        doctor_noshow_tendency = np.random.normal(0, 0.3)
        logit += doctor_noshow_tendency
        
        # Convert to probability
        prob_noshow = 1 / (1 + np.exp(-logit))
        noshow = np.random.binomial(1, prob_noshow)
        
        data.append({
            'department': dept,
            'doctor_id': doctor,
            'days_ahead': days_ahead,
            'hour': hour,
            'day_of_week': day_of_week,
            'month': month,
            'is_weekend': is_weekend,
            'patient_age': patient_age,
            'patient_gender': patient_gender,
            'patient_distance_km': patient_distance_km,
            'insurance_type': insurance_type,
            'total_past_appointments': total_past_appointments,
            'past_noshows': past_noshows,
            'past_cancellations': past_cancellations,
            'noshow_rate': noshow_rate,
            'days_since_last_visit': days_since_last_visit,
            'avg_lead_time': avg_lead_time,
            'is_followup': is_followup,
            'priority': priority,
            'appointment_type': appointment_type,
            'temperature': temperature,
            'precipitation': precipitation,
            'is_bad_weather': is_bad_weather,
            'received_reminder': received_reminder,
            'reminder_channel': reminder_channel,
            'confirmed_appointment': confirmed_appointment,
            'noshow': noshow
        })
    
    return pd.DataFrame(data)


def prepare_features(df):
    """Prepare features for training"""
    # Encode categorical variables
    le_dept = LabelEncoder()
    le_doctor = LabelEncoder()
    le_gender = LabelEncoder()
    le_insurance = LabelEncoder()
    le_reminder = LabelEncoder()
    le_appt_type = LabelEncoder()
    
    df['department_encoded'] = le_dept.fit_transform(df['department'])
    df['doctor_encoded'] = le_doctor.fit_transform(df['doctor_id'])
    df['gender_encoded'] = le_gender.fit_transform(df['patient_gender'])
    df['insurance_encoded'] = le_insurance.fit_transform(df['insurance_type'])
    df['reminder_encoded'] = le_reminder.fit_transform(df['reminder_channel'])
    df['appt_type_encoded'] = le_appt_type.fit_transform(df['appointment_type'])
    
    # Cyclical encoding
    df['hour_sin'] = np.sin(2 * np.pi * df['hour'] / 24)
    df['hour_cos'] = np.cos(2 * np.pi * df['hour'] / 24)
    df['day_sin'] = np.sin(2 * np.pi * df['day_of_week'] / 7)
    df['day_cos'] = np.cos(2 * np.pi * df['day_of_week'] / 7)
    df['month_sin'] = np.sin(2 * np.pi * df['month'] / 12)
    df['month_cos'] = np.cos(2 * np.pi * df['month'] / 12)
    
    # Log transform skewed features
    df['distance_log'] = np.log1p(df['patient_distance_km'])
    df['days_since_log'] = np.log1p(df['days_since_last_visit'])
    df['lead_time_log'] = np.log1p(df['avg_lead_time'])
    
    # Interaction features
    df['noshow_rate_x_appointments'] = df['noshow_rate'] * np.log1p(df['total_past_appointments'])
    df['reminder_x_confirm'] = df['received_reminder'] * df['confirmed_appointment']
    
    feature_cols = [
        'department_encoded', 'doctor_encoded',
        'days_ahead', 'hour_sin', 'hour_cos', 'day_sin', 'day_cos', 'month_sin', 'month_cos',
        'is_weekend',
        'patient_age', 'gender_encoded', 'distance_log', 'insurance_encoded',
        'total_past_appointments', 'past_noshows', 'past_cancellations', 'noshow_rate',
        'days_since_log', 'lead_time_log',
        'is_followup', 'priority', 'appt_type_encoded',
        'temperature', 'precipitation', 'is_bad_weather',
        'received_reminder', 'reminder_encoded', 'confirmed_appointment',
        'noshow_rate_x_appointments', 'reminder_x_confirm'
    ]
    
    X = df[feature_cols].values
    y = df['noshow'].values
    
    return X, y, feature_cols, {
        'dept': le_dept, 'doctor': le_doctor, 'gender': le_gender,
        'insurance': le_insurance, 'reminder': le_reminder, 'appt_type': le_appt_type
    }


def train_xgboost(X_train, y_train, X_val, y_val):
    """Train XGBoost classifier"""
    dtrain = xgb.DMatrix(X_train, label=y_train)
    dval = xgb.DMatrix(X_val, label=y_val)
    
    # Handle class imbalance
    scale_pos_weight = (y_train == 0).sum() / (y_train == 1).sum()
    
    params = {
        'objective': 'binary:logistic',
        'eval_metric': 'auc',
        'max_depth': 6,
        'learning_rate': 0.1,
        'subsample': 0.8,
        'colsample_bytree': 0.8,
        'min_child_weight': 3,
        'reg_alpha': 0.1,
        'reg_lambda': 1.0,
        'scale_pos_weight': scale_pos_weight,
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
    """Train LightGBM classifier"""
    train_data = lgb.Dataset(X_train, label=y_train)
    val_data = lgb.Dataset(X_val, label=y_val, reference=train_data)
    
    scale_pos_weight = (y_train == 0).sum() / (y_train == 1).sum()
    
    params = {
        'objective': 'binary',
        'metric': 'auc',
        'boosting_type': 'gbdt',
        'num_leaves': 31,
        'learning_rate': 0.1,
        'feature_fraction': 0.8,
        'bagging_fraction': 0.8,
        'bagging_freq': 5,
        'scale_pos_weight': scale_pos_weight,
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
    if 'xgb' in model_name.lower():
        import xgboost as xgb
        dtest = xgb.DMatrix(X_test)
        y_prob = model.predict(dtest)
    else:
        y_prob = model.predict(X_test)
    
    y_pred = (y_prob >= 0.5).astype(int)
    
    auc = roc_auc_score(y_test, y_prob)
    acc = accuracy_score(y_test, y_pred)
    prec = precision_score(y_test, y_pred)
    rec = recall_score(y_test, y_pred)
    f1 = f1_score(y_test, y_pred)
    
    print(f"\n{model_name} Performance:")
    print(f"  AUC:      {auc:.4f}")
    print(f"  Accuracy: {acc:.4f}")
    print(f"  Precision: {prec:.4f}")
    print(f"  Recall:   {rec:.4f}")
    print(f"  F1:       {f1:.4f}")
    
    # Confusion matrix
    cm = confusion_matrix(y_test, y_pred)
    print(f"  Confusion Matrix:")
    print(f"    TN: {cm[0,0]}, FP: {cm[0,1]}")
    print(f"    FN: {cm[1,0]}, TP: {cm[1,1]}")
    
    # Feature importance
    if hasattr(model, 'feature_importances_'):
        importance = model.feature_importances_
    elif hasattr(model, 'get_score'):
        importance_dict = model.get_score(importance_type='gain')
        importance = np.array([importance_dict.get(f'f{i}', 0) for i in range(len(feature_cols))])
    else:
        importance = None
    
    if importance is not None:
        top_features = sorted(zip(feature_cols, importance), key=lambda x: x[1], reverse=True)[:15]
        print(f"  Top 15 features:")
        for feat, imp in top_features:
            print(f"    {feat}: {imp:.4f}")
    
    return {
        'auc': auc, 'accuracy': acc, 'precision': prec, 
        'recall': rec, 'f1': f1, 'predictions': y_prob
    }


def save_model_artifacts(model, feature_cols, encoders, scaler, model_name, output_dir='models'):
    """Save model and preprocessing artifacts"""
    import os
    os.makedirs(output_dir, exist_ok=True)
    
    if 'xgb' in model_name.lower():
        model.save_model(f'{output_dir}/{model_name}.json')
    else:
        joblib.dump(model, f'{output_dir}/{model_name}.pkl')
    
    artifacts = {
        'feature_columns': feature_cols,
        'encoders': {k: v.classes_.tolist() for k, v in encoders.items()},
        'scaler_mean': scaler.mean_.tolist() if scaler else None,
        'scaler_scale': scaler.scale_.tolist() if scaler else None,
        'model_type': model_name,
        'threshold': 0.5,
        'created_at': datetime.now().isoformat()
    }
    
    with open(f'{output_dir}/{model_name}_artifacts.json', 'w') as f:
        json.dump(artifacts, f, indent=2)
    
    print(f"Saved {model_name} artifacts to {output_dir}/")


def main():
    print("=" * 60)
    print("No-Show Prediction Model Training")
    print("=" * 60)
    
    print("\nGenerating synthetic training data...")
    df = generate_synthetic_noshow_data(100000)
    print(f"Generated {len(df)} samples")
    print(f"No-show rate: {df['noshow'].mean():.2%}")
    
    print("\nPreparing features...")
    X, y, feature_cols, encoders = prepare_features(df)
    
    scaler = StandardScaler()
    X_scaled = scaler.fit_transform(X)
    
    X_train, X_temp, y_train, y_temp = train_test_split(X_scaled, y, test_size=0.3, random_state=42, stratify=y)
    X_val, X_test, y_val, y_test = train_test_split(X_temp, y_temp, test_size=0.5, random_state=42, stratify=y_temp)
    
    print(f"Train: {len(X_train)}, Val: {len(X_val)}, Test: {len(X_test)}")
    print(f"Train no-show rate: {y_train.mean():.2%}")
    
    print("\n" + "=" * 60)
    print("Training XGBoost...")
    print("=" * 60)
    xgb_model = train_xgboost(X_train, y_train, X_val, y_val)
    xgb_metrics = evaluate_model(xgb_model, X_test, y_test, 'XGBoost', feature_cols)
    save_model_artifacts(xgb_model, feature_cols, encoders, scaler, 'noshow_xgboost')
    
    print("\n" + "=" * 60)
    print("Training LightGBM...")
    print("=" * 60)
    lgb_model = train_lightgbm(X_train, y_train, X_val, y_val)
    lgb_metrics = evaluate_model(lgb_model, X_test, y_test, 'LightGBM', feature_cols)
    save_model_artifacts(lgb_model, feature_cols, encoders, scaler, 'noshow_lightgbm')
    
    best_model_name = 'XGBoost' if xgb_metrics['auc'] > lgb_metrics['auc'] else 'LightGBM'
    print(f"\nBest model: {best_model_name} (AUC: {max(xgb_metrics['auc'], lgb_metrics['auc']):.4f})")
    
    with open('models/best_noshow_model.txt', 'w') as f:
        f.write(best_model_name.lower().replace('xgboost', 'xgboost').replace('lightgbm', 'lightgbm'))
    
    print("\nTraining complete!")


if __name__ == '__main__':
    main()