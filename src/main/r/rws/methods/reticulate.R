# install.packages("reticulate")
library(reticulate)

# C:\Users\Administrator>conda env list
# # conda environments:
# #
# base                     D:\sliced\develop\anaconda\anaconda3
# PY3_11                   D:\sliced\develop\anaconda\anaconda3\envs\PY3_11
# ast-3.12                 D:\sliced\develop\anaconda\anaconda3\envs\ast-3.12
# ctp-3.11                 D:\sliced\develop\anaconda\anaconda3\envs\ctp-3.11
# ctp-3.12                 D:\sliced\develop\anaconda\anaconda3\envs\ctp-3.12
# ctp-ind-3.11             D:\sliced\develop\anaconda\anaconda3\envs\ctp-ind-3.11
# myinv                    D:\sliced\develop\anaconda\anaconda3\envs\myinv
# packt-ganstf2            D:\sliced\develop\anaconda\anaconda3\envs\packt-ganstf2

# use_python("D:/sliced/develop/anaconda/anaconda3/envs/ctp-3.11")
use_condaenv("PY3_11") 
np <- import("numpy")
