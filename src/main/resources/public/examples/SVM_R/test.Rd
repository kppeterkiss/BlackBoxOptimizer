#!/usr/bin/env Rscript
args = commandArgs(trailingOnly=TRUE)
library("kernlab")
library("lattice")
library("ggplot2")
library("caret")



if (length(args)<4) {
  stop("Usage : Rscript test.Rd dataset kernel c sigma\n", call.=FALSE)
}

df = read.csv(args[1], header = FALSE)
c = ncol(df)
r = nrow(df)
x = subset(df, select = ~-c)
y = df[[c]]

if(args[2]=="linear") {
	myC = args[3]
	train_control = trainControl(method = "cv", number = 10)
	grid = expand.grid(C = myC)
	model = train(x, y, trControl=train_control, method="svmLinear", tuneGrid = grid)
	cat(model$results$Rsquared)
	cat("\n")
}
if(args[2] == "rbf") {
	mySigma = args[4]
	train_control = trainControl(method = "cv", number = 10)
	grid = expand.grid(sigma = mySigma, C = 1)
	model = train(x, y, trControl=train_control, method="svmRadial", tuneGrid = grid)
	cat(model$results$Rsquared)
	cat("\n")
}



