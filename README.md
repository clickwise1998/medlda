

A java implementation of svm struct

refer codes:<br>
  http://www.cs.cornell.edu/People/tj/svm_light/svm_struct.html

refer papers:<br>
   Support Vector Machine Learning for Interdependent and Structured Output Spaces . Ioannis Tsochantaridis , Thomas Hofmann et. al. 2004 <br>
   Making Large-Scale SVM Learning Practical. Thorsten Joachims. 1998 <br>
   
Usage:
   build a jar file: ant <br>
   train: java  -cp click.jar cn.clickwise.classify.svm_struct.svm_struct_main -c 5000 example/train.txt example/model<br>
   test:   java  -cp click.jar cn.clickwise.classify.svm_struct.svm_struct_classify example/test.txt example/model example/prediction<br>
   
