#!/bin/bash
java -Xmx15000m -cp out/medlda.jar cn.clickwise.classify.svm_struct_pattern.svm_struct_main -c 5000 tbng/ntrain_ng.txt tbng/model_ng

#java -Xmx10000m -cp out/medlda.jar cn.clickwise.classify.svm_struct_pattern.svm_struct_classify tbng/ntest_ng.txt tbng/model_ng tbng/prediction


#java -Xmx10000m -cp out/medlda.jar cn.clickwise.medlda.Main est 120 566 10 16 64 temp random


#java -cp out/medlda.jar cn.clickwise.medlda.Main inf 4 temp110_c16_f10
