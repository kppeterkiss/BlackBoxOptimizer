suppressWarnings(suppressMessages(library(randomForest)))
suppressWarnings(suppressMessages(library(miscTools)))
options(warn=-1)

args = commandArgs(TRUE)
data <- read.csv("winequality-red.csv", header=TRUE, sep=";")

folds = 10

cols <- c('fixed.acidity', 'density', 'pH', 'alcohol')
rm = 0
  

for (i in 1:folds) {
  from <- ceiling(nrow(data)*((i-1)/folds))+1
  to <- ceiling(nrow(data)*(i/folds))
  range <- c(from:to)

  test = data[range,]
  train = data[-range,]
  
  rf <- randomForest(alcohol ~ ., data=train[,cols], ntree=as.numeric(args[1]), mtry=as.numeric(args[2]), nodesize=as.numeric(args[3]), maxnodes=as.numeric(args[4]) )
  r2 <- rSquared(test$alcohol, test$alcohol - predict(rf, train[,cols]))

	rm = rm - r2
}

rm = rm/folds
cat("accuracy", rm)

