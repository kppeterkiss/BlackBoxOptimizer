#source of the example: https://www.analyticsvidhya.com/blog/2016/01/complete-tutorial-ridge-lasso-regression-python/
import sys

#Importing libraries. The same will be used throughout the article.
import pandas as pd

#Define input array with angles from 60deg to 300deg converted to radians


from sklearn.linear_model import Lasso

def lasso_regression(data, predictors, alpha, models_to_plot={}):
    #Fit the model
    lassoreg = Lasso(alpha=alpha,normalize=True, max_iter=1e5)
    lassoreg.fit(data[predictors],data['y'])
    y_pred = lassoreg.predict(data[predictors])
    #Return the result in pre-defined format
    rss = sum((y_pred-data['y'])**2)
    return rss

alpha_lasso = float(sys.argv[1])
data = pd.read_csv('data.csv')

#Initialize predictors to all 15 powers of x
predictors=['x']
predictors.extend(['x_%d'%i for i in range(2,16)])


#Initialize the dataframe to store coefficients
col = ['rss','intercept'] + ['coef_x_%d'%i for i in range(1,16)]
ind = ['alpha_%.2g'%alpha_lasso for i in range(0,10)]
coef_matrix_lasso = pd.DataFrame(index=ind, columns=col)


print("rss", lasso_regression(data, predictors, alpha_lasso) )