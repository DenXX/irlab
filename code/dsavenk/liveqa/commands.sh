mvn clean
mvn compile
mvn exec:java -Dexec.mainClass="edu.emory.mathcs.ir.utilapps.DownloadQnAByIdApp" -Dexec.args="/home/dsavenk/ir/data/liveqa/liveqa15-trec-qids-only.txt /home/dsavenk/ir/data/liveqa/liveqa15-trec-qna.txt"
mvn exec:java -Dexec.mainClass="edu.emory.mathcs.ir.qa.LiveQaServer"
