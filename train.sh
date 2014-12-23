#!/bin/bash
#java -Xmx10000m -cp out/medlda.jar cn.clickwise.classify.svm_struct_pattern.svm_struct_main -c 5000 tbmul/nsltest_tb.txt tbmul/model

java -Xmx10000m -cp out/medlda.jar cn.clickwise.classify.svm_struct_pattern.svm_struct_classify tbmul/nsltrain_tb.txt tbmul/model tbmul/prediction


#java -Xmx100m -cp out/medlda.jar cn.clickwise.medlda.Main est 110 4 10 16 64 temp random


#java -cp out/medlda.jar cn.clickwise.medlda.Main inf 4 temp110_c16_f10
