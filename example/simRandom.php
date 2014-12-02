<?php

$stdin = fopen('php://stdin','r');
if(false===$stdin)
{
   fprintf(STDERR,"can not open stdin \n");
   exit(1);
}
$line="";
$oldtr_file="";
$newtr_file="";
$newte_file="";
while(!feof($stdin))
{
   $line=trim(fgets($stdin,4096*64));
   if(strlen($line)>0)
   { 
     $seg_arr=split("[ \t]",$line);
    for($i=0;$i<count($seg_arr);$i++)
    {
      printf("seg_arr[%d]=%s \n",$i,$seg_arr[$i]);
    }
    if(count($seg_arr)!=3)
    {
     echo "输入词典路径和host文件路径,中间用空格分开\n";
    }
    else
    {
     $oldtr_file=$seg_arr[0];
     $newtr_file=$seg_arr[1];
     $newte_file=$seg_arr[2];
    }
   }
} 

//echo "dict_file:".$dict_file."\n";
//echo "nout_file:".$nout_file."\n";

echo "oldtr_file:".$oldtr_file."\n";
echo "newtr_file:".$newtr_file."\n";
echo "newte_file:".$newte_file."\n";


$old_in=fopen($oldtr_file,'r');
$newtr_out=fopen($newtr_file,'w');
$newte_out=fopen($newte_file,'w');
$ran_num=rand(0,20);
while(!feof($old_in))
{
  $line=trim(fgets($old_in,4096*16));
  if(strlen($line)>2)
 {
  $line=$line."\n";
  $ran_num=rand(0,200);
  if($ran_num>130)
  {
   fwrite($newte_out,$line);
  }
  else if($ran_num<130)
  {
   fwrite($newtr_out,$line);
  }
 }
}


fclose($old_in);
fclose($newtr_out);
fclose($newte_out); 
?>
