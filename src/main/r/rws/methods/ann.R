pkgs <- strsplit("reticulate,keras", ",") |> unlist()
flags <- pkgs |> sapply(require, character.only=T)
pkgs[!flags] |> lapply(install.packages, character.only=T)

mnist <- dataset_mnist()

x_train <- mnist$train$x
y_train <- mnist$train$y

x_test <- mnist$test$x
y_test <- mnist$test$y

x_train <- array_reshape(x_train, c(nrow(x_train), 784))
x_test <- array_reshape(x_train, c(nrow(x_test), 784))

y_train <- to_categorical(y_train, 10)
y_train <- to_categorical(y_test, 10)