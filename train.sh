#!/bin/bash
#java -Xmx15000m -cp out/medlda.jar cn.clickwise.classify.svm_struct_pattern.svm_struct_main -c 5000 tbmul_1224/nltrain_tb_1224.txt tbmul_1224/model_1224

java -Xmx10000m -cp out/medlda.jar cn.clickwise.classify.svm_struct_pattern.svm_struct_classify tbmul_1224/nltest_tb_1224.txt tbmul_1224/model_1224 tbmul_1224/prediction


#java -Xmx100m -cp out/medlda.jar cn.clickwise.medlda.Main est 110 4 10 16 64 temp random


#java -cp out/medlda.jar cn.clickwise.medlda.Main inf 4 temp110_c16_f10
