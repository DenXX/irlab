
[TOC]

# 23 September 2015
 ## TODO:
 - Scrape Bing Web Search API with WebQuestions questions
 - Study how often the answer appears in snippets
 
# 25 September 2015
 
 Results of the first evaluation run of the AQQU:
 2015-09-25 11:48:18,697 : INFO : learner : Average results over 1 runs: 
 2015-09-25 11:48:18,697 : INFO : learner : accuracy: 0.3701
 2015-09-25 11:48:18,715 : INFO : learner : avg_f1: 0.4962
 2015-09-25 11:48:18,738 : INFO : learner : avg_num_candidates: 30.3031
 2015-09-25 11:48:18,774 : INFO : learner : avg_oracle_position: 2.7174
 2015-09-25 11:48:18,793 : INFO : learner : avg_precision: 0.4832
 2015-09-25 11:48:18,795 : INFO : learner : avg_precision_xao: 0.4919
 2015-09-25 11:48:18,796 : INFO : learner : avg_recall: 0.6062
 2015-09-25 11:48:18,817 : INFO : learner : f1_kw: 0.3734
 2015-09-25 11:48:18,818 : INFO : learner : macro_f1: 0.5378
 2015-09-25 11:48:18,820 : INFO : learner : macro_f1_xao: 0.5431
 2015-09-25 11:48:18,821 : INFO : learner : num_questions: 2032.0000
 2015-09-25 11:48:18,821 : INFO : learner : num_questions_no_answer: 0.0000
 2015-09-25 11:48:18,821 : INFO : learner : oracle_accuracy: 0.5359
 2015-09-25 11:48:18,821 : INFO : learner : oracle_avg_f1: 0.6861
 2015-09-25 11:48:18,822 : INFO : learner : oracle_top_10: 0.8248
 2015-09-25 11:48:18,822 : INFO : learner : oracle_top_100: 0.8671
 2015-09-25 11:48:18,822 : INFO : learner : oracle_top_2: 0.6836
 2015-09-25 11:48:18,822 : INFO : learner : oracle_top_3: 0.7377
 2015-09-25 11:48:18,822 : INFO : learner : oracle_top_5: 0.7761
 2015-09-25 11:48:18,822 : INFO : learner : precision_kw: 0.3768
 2015-09-25 11:48:18,822 : INFO : learner : recall_kw: 0.3701

# 28 September 2015
TODO:
- figure out how different feature sets are used in Accu
- download web pages for serps scraped for Webquestions questions

Commands:
> sbt compile
> sbt -mem 180000 "run-main edu.emory.mathcs.ir.DownloadSerpDocumentsApp /home/dsavenk/ir/data/WebQuestions/webquestions.examples.train.bing_results.json /home/dsavenk/ir/data/WebQuestions/bing_results_documents/ /home/dsavenk/ir/data/WebQuestions/webquestions.examples.train.bing_results_documents.json"
> sbt -mem 180000 "run-main edu.emory.mathcs.ir.DownloadSerpDocumentsApp /home/dsavenk/ir/data/WebQuestions/webquestions.examples.test.bing_results.json /home/dsavenk/ir/data/WebQuestions/bing_results_documents/ /home/dsavenk/ir/data/WebQuestions/webquestions.examples.test.bing_results_documents.json"

# 29 September 2015
TODO:
- integrate text-based feature generation into the ACCU code
- run training and testing

Commands:
> python -m query_translator.learner train WQ_Ranker_WithText
> python -m query_translator.learner test WQ_Ranker_WithText webquestionstest

# 30 September 2015
TODO:
- fix the problems with web search results based feature generation
- run experiments on training
- start implementing AskMsr+

# 1 October 2015

Training time:
real    1234m54.204s
user    1235m47.546s
sys     21m25.973s
Testing time:
real    27m46.950s
user    51m35.628s
sys     1m20.537s


Results with simple text-based features (including for pruning): 
2015-10-01 14:51:26,048 : INFO : learner : accuracy: 0.3829
2015-10-01 14:51:26,048 : INFO : learner : avg_f1: 0.5084
2015-10-01 14:51:26,048 : INFO : learner : avg_num_candidates: 15.5049
2015-10-01 14:51:26,048 : INFO : learner : avg_oracle_position: 1.8174
2015-10-01 14:51:26,048 : INFO : learner : avg_precision: 0.4943
2015-10-01 14:51:26,048 : INFO : learner : avg_precision_xao: 0.5101
2015-10-01 14:51:26,049 : INFO : learner : avg_recall: 0.6223
2015-10-01 14:51:26,049 : INFO : learner : f1_kw: 0.3889
2015-10-01 14:51:26,049 : INFO : learner : macro_f1: 0.5510
2015-10-01 14:51:26,049 : INFO : learner : macro_f1_xao: 0.5607
2015-10-01 14:51:26,049 : INFO : learner : num_questions: 2032.0000
2015-10-01 14:51:26,049 : INFO : learner : num_questions_no_answer: 0.0000
2015-10-01 14:51:26,049 : INFO : learner : oracle_accuracy: 0.5143
2015-10-01 14:51:26,049 : INFO : learner : oracle_avg_f1: 0.6586
2015-10-01 14:51:26,049 : INFO : learner : oracle_top_10: 0.8219
2015-10-01 14:51:26,049 : INFO : learner : oracle_top_100: 0.8302
2015-10-01 14:51:26,049 : INFO : learner : oracle_top_2: 0.6998
2015-10-01 14:51:26,049 : INFO : learner : oracle_top_3: 0.7530
2015-10-01 14:51:26,049 : INFO : learner : oracle_top_5: 0.7923
2015-10-01 14:51:26,049 : INFO : learner : precision_kw: 0.3951
2015-10-01 14:51:26,049 : INFO : learner : recall_kw: 0.3829

Results with simple text-based features and same pruning:
2015-10-01 21:23:41,988 : INFO : learner : accuracy: 0.3775
2015-10-01 21:23:41,988 : INFO : learner : avg_f1: 0.5008
2015-10-01 21:23:41,988 : INFO : learner : avg_num_candidates: 55.3548
2015-10-01 21:23:41,988 : INFO : learner : avg_oracle_position: 3.0509
2015-10-01 21:23:41,988 : INFO : learner : avg_precision: 0.4870
2015-10-01 21:23:41,988 : INFO : learner : avg_precision_xao: 0.4935
2015-10-01 21:23:41,988 : INFO : learner : avg_recall: 0.6121
2015-10-01 21:23:41,988 : INFO : learner : f1_kw: 0.3800
2015-10-01 21:23:41,988 : INFO : learner : macro_f1: 0.5424
2015-10-01 21:23:41,988 : INFO : learner : macro_f1_xao: 0.5464
2015-10-01 21:23:41,988 : INFO : learner : num_questions: 2032.0000
2015-10-01 21:23:41,989 : INFO : learner : num_questions_no_answer: 0.0000
2015-10-01 21:23:41,989 : INFO : learner : oracle_accuracy: 0.5300
2015-10-01 21:23:41,989 : INFO : learner : oracle_avg_f1: 0.6807
2015-10-01 21:23:41,989 : INFO : learner : oracle_top_10: 0.8130
2015-10-01 21:23:41,989 : INFO : learner : oracle_top_100: 0.8612
2015-10-01 21:23:41,989 : INFO : learner : oracle_top_2: 0.6811
2015-10-01 21:23:41,989 : INFO : learner : oracle_top_3: 0.7338
2015-10-01 21:23:41,989 : INFO : learner : oracle_top_5: 0.7741
2015-10-01 21:23:41,989 : INFO : learner : precision_kw: 0.3825
2015-10-01 21:23:41,989 : INFO : learner : recall_kw: 0.3775