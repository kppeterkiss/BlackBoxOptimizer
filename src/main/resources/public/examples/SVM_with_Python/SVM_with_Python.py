from sklearn import svm
from sklearn import metrics
from sklearn.metrics import average_precision_score
import numpy as np
import pandas as pd
from sklearn.metrics import recall_score
import arff
import sys
from os import listdir
from os.path import isfile, join
from sklearn import preprocessing
from sklearn.metrics import cohen_kappa_score
from sklearn.metrics import  roc_auc_score
from sklearn.neighbors import KNeighborsClassifier
from sklearn.metrics import matthews_corrcoef
import warnings
import csv

def balanced_accuracy(result):
    all_classes = list(set(result['class'].values))
    all_class_accuracies = []
    for this_class in all_classes:
        this_class_accuracy = len(result[(result['guess'] == this_class) & (result['class'] == this_class)])            / float(len(result[result['class'] == this_class]))
        all_class_accuracies.append(this_class_accuracy)

    balanced_accuracy = np.mean(all_class_accuracies)

    return balanced_accuracy

def dummies(df):
    new_columns = df.columns.values
    qa= (df.iloc[:,-1:]).values
    lb = preprocessing.LabelBinarizer()
    df[new_columns[len(new_columns)-1]]=lb.fit_transform(qa)
    for i in range(len(new_columns)-1):
        if not np.issubdtype(df[new_columns[i]].dtype, np.number):
            dummies = pd.get_dummies(df[new_columns[i]]).rename(columns=lambda x: new_columns[i]+ "_"+ str(x))
            df = df.drop(new_columns[i], 1)
            df = pd.concat([df, dummies], axis=1) 
    classc=df[['Class']]
    df = df.drop('Class', 1)
    df = pd.concat([df, classc], axis=1) 
    return df

def split_positive_negative(dataset):
    pdata=[]
    ndata=[]
    sdata= dataset
    pdata=sdata[sdata['Class']==1]
    ndata=sdata[sdata['Class']==0]
    return (pdata,ndata)

def k_folds(ndata,pdata):
    lp=pdata.shape[0]
    ln=ndata.shape[0]   
    if lp<ln:
        if lp>=5:
            nfolds=5
        else:
            nfolds=lp
    else:
        if ln>=5:
            nfolds=5
        else:
            nfolds=ln
    avg1 =  ln / nfolds
    avg2 =  lp / nfolds
    out1 = []
    out2=[]
    finalout2=[]
    last1 = 0
    last2=0
    i=0
    while last1 <  ln:
        out1.append(ndata[int(last1):int(last1 + avg1)])
        last1 += avg1
 
    while last2 <  lp:
        out2.append(pdata[int(last2):int(last2 + avg2)])
        last2 += avg2
    for i in range(len(out1)):
        f=pd.concat([out1[i],out2[i]])
        finalout2.append(f)
    return (finalout2,nfolds)

def svm_computation(X_train_folds,y_train_folds,file,num_folds,kernal,c,gamma,degree):        
    warnings.filterwarnings('ignore', category=UserWarning, append=True)
    warnings.filterwarnings('ignore', category=RuntimeWarning, append=True)
    f=[]
    i = 0
    a_accuracy=0 
    HEADER = ['fold','kernal','C','gamma','Degree','accuracy','balance_accuracy',
              'Average_precision_recall','precision','recall',
              'F_measure','Classification_auc',
              'matthews_corr','coh_kapp']
    
    for fold in range(num_folds):
        for k in kernal:
            if k == 'linear':
                for cval in c:
                    validation_X_test = X_train_folds[fold]
                    validation_X_test=np.array(validation_X_test)
                    validation_y_test = y_train_folds[fold]
                    validation_y_test=np.array(validation_y_test)
                    temp_X_train = np.concatenate(X_train_folds[:fold] + X_train_folds[fold + 1:])
                    temp_y_train = np.concatenate(y_train_folds[:fold] + y_train_folds[fold + 1:])
                    
                    lin_svc = svm.LinearSVC(C=cval)
                    
                    
                    lin_svc.fit( temp_X_train, temp_y_train.ravel())
                    temp_y_test_pred = lin_svc.predict(validation_X_test)
            
                    y_score = lin_svc.decision_function(validation_X_test)
                    Average_precision_recall = average_precision_score(validation_y_test.ravel(), y_score)
                    accuracy=metrics.accuracy_score(validation_y_test.ravel(), temp_y_test_pred)
                                        
                    Classification_auc = roc_auc_score(validation_y_test.ravel(), y_score)
                    F_measure=metrics.f1_score(validation_y_test.ravel(), temp_y_test_pred) 
                    matthews_corr=matthews_corrcoef(validation_y_test.ravel(), temp_y_test_pred)  
                    coh_kapp=cohen_kappa_score(validation_y_test.ravel(),temp_y_test_pred)
                    balance_accuracy=recall_score(validation_y_test.ravel(),temp_y_test_pred, average='macro') 

                    precision=metrics.precision_score(validation_y_test.ravel(), temp_y_test_pred)
                    recall=metrics.recall_score(validation_y_test.ravel(), temp_y_test_pred)
                    i+=1
                    a_accuracy+=accuracy
                    print('accuracy_1 '+ str(accuracy))

                    
                    
                    
                    
            if k=='rbf':
                for cval in c:
                    for g in gamma:
                        validation_X_test = X_train_folds[fold]
                        validation_X_test=np.array(validation_X_test)
                        validation_y_test = y_train_folds[fold]
                        validation_y_test=np.array(validation_y_test)
                        temp_X_train = np.concatenate(X_train_folds[:fold] + X_train_folds[fold + 1:])
                        temp_y_train = np.concatenate(y_train_folds[:fold] + y_train_folds[fold + 1:])
                        
                        rbf_svc = svm.SVC(kernel='rbf', gamma=g, C=cval)
                        
                        rbf_svc.fit( temp_X_train, temp_y_train.ravel())
                        temp_y_test_pred = rbf_svc.predict(validation_X_test) 
                        
                        y_score = rbf_svc.decision_function(validation_X_test)
                        Average_precision_recall = average_precision_score(validation_y_test.ravel(), y_score)
                        accuracy=metrics.accuracy_score(validation_y_test.ravel(), temp_y_test_pred)  
                        Classification_auc = roc_auc_score(validation_y_test.ravel(), y_score)
                        F_measure=metrics.f1_score(validation_y_test.ravel(), temp_y_test_pred) 
                        matthews_corr=matthews_corrcoef(validation_y_test.ravel(), temp_y_test_pred)  
                        coh_kapp=cohen_kappa_score(validation_y_test.ravel(),temp_y_test_pred)
                        balance_accuracy=recall_score(validation_y_test.ravel(),temp_y_test_pred, average='macro')
                        precision=metrics.precision_score(validation_y_test.ravel(), temp_y_test_pred)
                        recall=metrics.recall_score(validation_y_test.ravel(), temp_y_test_pred)
                        i+=1
                        a_accuracy+=accuracy
                        print('accuracy_1 '+ str(accuracy))

                        
                        
                        
            if k=='poly':
                for cval in c:
                    for d in degree:
                        for g in gamma:
                            validation_X_test = X_train_folds[fold]
                            validation_X_test=np.array(validation_X_test)
                            validation_y_test = y_train_folds[fold]
                            validation_y_test=np.array(validation_y_test)
                            temp_X_train = np.concatenate(X_train_folds[:fold] + X_train_folds[fold + 1:])
                            temp_y_train = np.concatenate(y_train_folds[:fold] + y_train_folds[fold + 1:])
                            
                            poly_svc = svm.SVC(kernel='poly', degree=d, C=cval,gamma=g)
                            
                            poly_svc.fit( temp_X_train, temp_y_train.ravel())
                            temp_y_test_pred = poly_svc.predict(validation_X_test) 
                            y_score = poly_svc.decision_function(validation_X_test)
                            Average_precision_recall = average_precision_score(validation_y_test.ravel(), y_score)
                            accuracy=metrics.accuracy_score(validation_y_test.ravel(), temp_y_test_pred)   
                            Classification_auc = roc_auc_score(validation_y_test.ravel(), y_score)
                            F_measure=metrics.f1_score(validation_y_test.ravel(), temp_y_test_pred) 
                            matthews_corr=matthews_corrcoef(validation_y_test.ravel(), temp_y_test_pred)  
                            coh_kapp=cohen_kappa_score(validation_y_test.ravel(),temp_y_test_pred)
                            balance_accuracy=recall_score(validation_y_test.ravel(),temp_y_test_pred, average='macro')
                            precision=metrics.precision_score(validation_y_test.ravel(), temp_y_test_pred)
                            recall=metrics.recall_score(validation_y_test.ravel(), temp_y_test_pred)
                            i+=1
                            a_accuracy+=accuracy
                            print('accuracy_1 '+ str(accuracy))

                        
            if k=='sigmoid':
                for cval in c:
                    for g in gamma:
                        validation_X_test = X_train_folds[fold]
                        validation_X_test=np.array(validation_X_test)
                        validation_y_test = y_train_folds[fold]
                        validation_y_test=np.array(validation_y_test)
                        temp_X_train = np.concatenate(X_train_folds[:fold] + X_train_folds[fold + 1:])
                        temp_y_train = np.concatenate(y_train_folds[:fold] + y_train_folds[fold + 1:])
                        
                        svc_sig = svm.SVC(kernel='sigmoid', gamma=g, C=cval)
                        
                        svc_sig.fit( temp_X_train, temp_y_train.ravel())
                        temp_y_test_pred = svc_sig.predict(validation_X_test) 
                        y_score = svc_sig.decision_function(validation_X_test)
                        Average_precision_recall = average_precision_score(validation_y_test.ravel(), y_score)
                        accuracy=metrics.accuracy_score(validation_y_test.ravel(), temp_y_test_pred)  
                        Classification_auc = roc_auc_score(validation_y_test.ravel(), y_score)
                        F_measure=metrics.f1_score(validation_y_test.ravel(), temp_y_test_pred) 
                        matthews_corr=matthews_corrcoef(validation_y_test.ravel(), temp_y_test_pred)  
                        coh_kapp=cohen_kappa_score(validation_y_test.ravel(),temp_y_test_pred)
                        balance_accuracy=recall_score(validation_y_test.ravel(),temp_y_test_pred, average='macro')
                        precision=metrics.precision_score(validation_y_test.ravel(), temp_y_test_pred)
                        recall=metrics.recall_score(validation_y_test.ravel(), temp_y_test_pred)
                        
                        i+=1
                        a_accuracy+=accuracy
                        print('accuracy_1 '+ str(accuracy))
            
        
    #f = open('obj.txt', 'w')
    print('accuracy '+ str(a_accuracy/i))
    #f.write('accuracy '+ str(a_accuracy/i))
    
def read_data(file_path):
    data=arff.load(open(file_path, 'r'))
    d=pd.DataFrame(data['data'], columns=[attribute[0] for attribute in data['attributes']])
    return (d)

def calculate_for_one_db_svm(file_name_with_path,result_file_name_with_path): 
    c1=np.logspace(-3, 4,100,base=10)
    C=c1
    g1=np.logspace(-3, 4,100,base=10)
    gamma=g1
    kernal=['linear','rbf','poly']
    degree=[1,2,3,4,5]
    data = read_data(file_name_with_path)
    df_new=dummies(data)
    pdata,ndata=split_positive_negative(df_new)
    pxdata=pdata.iloc[:,:-1]
    pydata=pdata.iloc[:,-1:]
    nxdata=ndata.iloc[:,:-1]
    nydata=ndata.iloc[:,-1:]
    X_train_folds,nfolds=k_folds(nxdata,pxdata)
    y_train_folds,nfolds=k_folds(nydata,pydata)
    svm_computation(X_train_folds,y_train_folds,result_file_name_with_path,nfolds,kernal,C,gamma,degree)
    
    
def run_one(file_name_with_path,kernel, c, gamma ): 
    C=[]
    C.append(c)
    g1=[]
    g1.append(gamma)
    kernal=[]
    kernal.append(kernel)
    degree=[3]
    data = read_data(file_name_with_path)
    df_new=dummies(data)
    pdata,ndata=split_positive_negative(df_new)
    pxdata=pdata.iloc[:,:-1]
    pydata=pdata.iloc[:,-1:]
    nxdata=ndata.iloc[:,:-1]
    nydata=ndata.iloc[:,-1:]
    X_train_folds,nfolds=k_folds(nxdata,pxdata)
    y_train_folds,nfolds=k_folds(nydata,pydata)
    accuracy = svm_computation(X_train_folds,y_train_folds,"x",nfolds,kernal,C,g1,degree)
    

def create_data_file_list(path):
        res = [f for f in listdir(path) if isfile(join(path, f)) and '.arff' in f]
        return res
    
def create_result_file_list_svm(data_file_list):
        res = [f[:-5]+"-svm" for f in data_file_list]
        return res
    
def append_path_to_file_list(file_list,path):
        res = [path+f for f in file_list]
        return res





def trial():
    path="./data/"
    res_path="./output/final_svm_results/"
    df_file_list = create_data_file_list(path)
    res_file_list = create_result_file_list_svm(df_file_list)
    df_file_list = append_path_to_file_list(df_file_list,path)
    res_file_list = append_path_to_file_list(res_file_list,res_path)
    for i in range(len(df_file_list)):
        calculate_for_one_db_svm(df_file_list[i],res_file_list[i])

df_file = sys.argv[1]
kernel = sys.argv[2]
gamma =  sys.argv[3]
c = sys.argv[4]
print(sys.argv[0])
print(sys.argv)
run_one(df_file,kernel,float(c),float(gamma)    )


# In[ ]:



