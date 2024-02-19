suppressWarnings(suppressMessages(library(rpart)))

# ----------------------
# Functions
# ----------------------

compute_accuracy <- function(real, predicted) {
  sum = 0
  for (i in 1:length(real)) {
    if (real[i] == predicted[i]) {
	  sum = sum + 1
    }
  }
  return (sum/length(real))
}

# ----------------------
# optimizer.main.Main program
# ----------------------
# args[1] = input file -- string
# args[2] = cp -- float
# args[3] = minsplit -- integer
# args[4] = minbucket -- integer
# args[5] = maxdepth -- integer
# args[6] = usesurrogate -- integer {0,1,2}
# args[7] = surrogatestyle -- integer {0,1} (not boolean)
# args[8] = maxsurrogate -- integer
# args[9] = maxcompete -- integer
# args[10] = xval -- integer
# defaults = iris.txt 0.01 20 7 30 2 0 5 4 10 

args = commandArgs(TRUE)
data <- read.csv(args[1], header=TRUE)

folds = 10
avg_accuracy = 0

for (i in 1:folds) {
  from <- ceiling(nrow(data)*((i-1)/folds))+1
  to <- ceiling(nrow(data)*(i/folds))
  range <- c(from:to)

  test.data = data[range,]
  train.data = data[-range,]

  if (length(unique(train.data[,"Class"])) > 1) {
    result_rpart <- rpart(as.factor(Class)~., data = train.data, method = "class", 
						  control = rpart.control(cp = as.numeric(args[2]), 
					  			  		minsplit = as.numeric(args[3]), 
					  			  		minbucket = as.numeric(args[4]), 
					  			  		maxdepth = as.numeric(args[5]), 
						  			    usesurrogate = as.numeric(args[6]), 
					  			  		surrogatestyle = as.numeric(args[7]), 
					  			  		maxsurrogate = as.numeric(args[8]),
					  			  		maxcompete = as.numeric(args[9]),
					  			  		xval = as.numeric(args[10])))

    predictions <- predict(result_rpart, test.data, type="class")

	accuracy <- compute_accuracy(test.data$Class, predictions)
	avg_accuracy = avg_accuracy + accuracy
  }
}

avg_accuracy = avg_accuracy/folds
cat("accuracy", avg_accuracy)

