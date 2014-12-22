#!/bin/bash
#java -Xmx10000m -cp out/medlda.jar cn.clickwise.classify.svm_struct.svm_struct_main -c 5000 tb/nstest_tb.txt tb/model

java -Xmx10000m -cp out/medlda.jar cn.clickwise.classify.svm_struct.svm_struct_classify tb/nstrain_tb.txt tb/model tb/prediction


#java -Xmx100m -cp out/medlda.jar cn.clickwise.medlda.Main est 110 4 10 16 64 temp random


#java -cp out/medlda.jar cn.clickwise.medlda.Main inf 4 temp110_c16_f10
