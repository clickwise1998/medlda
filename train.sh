#!/bin/bash
java -Xmx10000m -cp out/medlda.jar cn.clickwise.classify.svm_struct_pattern.svm_struct_main -c 5000 tbmuls/nstest_tbtst.txt tbmuls/model

#java -Xmx10000m -cp out/medlda.jar cn.clickwise.classify.svm_struct_pattern.svm_struct_classify tbmuls/nstrain_tbtst.txt tbmuls/model tbmuls/prediction


#java -Xmx100m -cp out/medlda.jar cn.clickwise.medlda.Main est 110 4 10 16 64 temp random


#java -cp out/medlda.jar cn.clickwise.medlda.Main inf 4 temp110_c16_f10
