#!/bin/bash
#java -Xmx15000m -cp out/medlda.jar cn.clickwise.classify.sspm.svm_struct_main -c 5000 tb_1224_ngram/nltrain_1224_ngram.txt tb_1224_ngram/model_1224_lngram

java -Xmx10000m -cp out/medlda.jar cn.clickwise.classify.sspm.svm_struct_classify tb_1224_ngram/nltest_1224_ngram.txt tb_1224_ngram/model_1224_lngram tb_1224_ngram/prediction


#java -Xmx100m -cp out/medlda.jar cn.clickwise.medlda.Main est 110 4 10 16 64 temp random


#java -cp out/medlda.jar cn.clickwise.medlda.Main inf 4 temp110_c16_f10
