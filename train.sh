#!/bin/bash
java -Xmx30000m -cp out/medlda.jar cn.clickwise.classify.sspm.svm_struct_main -c 5000 tbng/nltrain_ng.txt tbng/model_ng

#java -Xmx10000m -cp out/medlda.jar cn.clickwise.classify.sspm.svm_struct_classify tbng/nltest_ng.txt tbng/model_ng tbng/prediction_ng


#java -Xmx10000m -cp out/medlda.jar cn.clickwise.medlda.Main est 120 566 10 16 64 temp random


#java -cp out/medlda.jar cn.clickwise.medlda.Main inf 4 temp110_c16_f10
