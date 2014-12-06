#!/bin/bash
#java -Xmx4000m -cp jmlt.jar org.jmlp.classify.svm_struct.source.svm_struct_main -c 5000 example/train.txt example/model

#java -Xmx2000m -cp jmlt.jar org.jmlp.classify.svm_struct.source.svm_struct_classify example/test.txt example/model example/prediction


#java -Xmx100m -cp out/medlda.jar cn.clickwise.medlda.Main est 100 4 10 16 64 temp random


java -cp out/medlda.jar cn.clickwise.medlda.Main inf 4 temp100_c16_f10
