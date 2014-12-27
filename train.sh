#!/bin/bash
#java -Xmx15000m -cp out/medlda.jar cn.clickwise.classify.sspm.svm_struct_main -c 5000 example/train.txt example/model

java -Xmx10000m -cp out/medlda.jar cn.clickwise.classify.sspm.svm_struct_classify example/test.txt example/model example/prediction


#java -Xmx10000m -cp out/medlda.jar cn.clickwise.medlda.Main est 120 566 10 16 64 temp random


#java -cp out/medlda.jar cn.clickwise.medlda.Main inf 4 temp110_c16_f10
