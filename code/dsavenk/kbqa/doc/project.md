[TOC]

# Motivation
The main problem in Knowledge Base Question Answering (KBQA) is translation of natural language question and its constituents into the structured query representation. Systems needs to have some kind of lexicon, which is usually obtained during training from the training set of QnA pairs or from some other resources, such as relation phrases (distant supervision), question paraphrases, etc. Existing systems do not use any lexical resources during training. However, as we know text based question answering systems work pretty good and were shown (Sun et al 2015 - "Open Domain Question Answering via Semantic Enrichment") to perform better on general QA datasets, such as TREC QA. The idea of this work is to adapt text-based QA techniques to improve question answering over structured data.

# Datasets

* [FREE917](http://www-nlp.stanford.edu/software/sempre/) - manually created questions
* [WebQuestions](http://www-nlp.stanford.edu/software/sempre/) - questions collected through Google suggest and with answers obtained through Mechanical Turk. All questions can be answered with Freebase.
* [QALD challenge](http://greententacle.techfak.uni-bielefeld.de/~cunger/qald/)

# Related work

## WebQuestions dataset

### Metric
F1 reported in all works (except Yao and Van Durme) is average F-1 over all questions. It is like accuracy with partial credit.
The script provided for evaluation also prints average precision, average recall and F1 of average precision and recall.
If no answers are provided precision is considered to be 1. It has no effect on average F1, but has on F1 of average precision and recall.

| System | F1 |
|-----------------------------------------|-----------|
| 1) Berant et al 2013 (SEMPRE) | 31.4 |
| 2) Bordes et al 2013 | 29.7 |
| 3) Fader et al 2014 (Paralex) | 35.0 |
| 4) Yao and Van Durme, 2014 | 33.0 |
| 5) Berant and Liang, 2014 (ParaSEMPRE) | 39.9 |
| 6) Bordes et al 2014 | 39.2 |
| 7) Ensemble of 6 and 5 | 41.8 |
| 8) DeepQA from MSFT | 45.3 (77.3 upper bound) |
| 9) Lean QA (Yao)          | 44.3 |
| 10) Aqqu (Bass, Haussman) | 49.4 | 
| 11) STAGG from MSFT | 52.5 | 
|-----------------------------------------|-----------|

1. **J. Berant, A. Chou, R. Frostig, and P. Liang. Semantic parsing on Freebase from question-answer pairs**, EMNLP 2013
semantic parsing - building semantic representation for a question or statement. Method first builds lexicon phrase->freebase predicate, by considering entities that coocur frequently with phrase and the predicate. Then a set of logical forms is built bottom-up, starting with phrases we use lexicon to map them to a set of predicates, then we combine neighboring predicates using several possible operations and on each step we score current result and keep beam of k. Combination operations include join, intersection and bridging, where we ignore words and simply try to combine 2 neighboring predicates using all predicates that support these types and these entities. Each candidates comes with a set of features that is later judged. Also features include the results of applying this logical form/query to freebase, e.g. how many things match. Real word statements tend to have ~1 matches
Model is called SemPre and its source code is available.
2. **A. Fader, L. Zettlemoyer, and O. Etzioni. Paraphrase-driven learning for open question answering**, ACL 2013
3. **X. Yao and B. Van Durme. Information extraction over structured data: Question answering with Freebase**, ACL, 2014
The paper presents a QA system based on Freebase. Questions are parsed using stanford dependency parser. Then we build features for edges of this dependency graph, this graph is slightly converted by recognizing qword (question word), qverb (main verb), qfocus (word identifying type of answer), qtopic (using NER), this can also be viewed as a graph and for each edge we extract a feature (seems to be Boolean presence features), take a look at the example for details. Qtopic is used to extract a part of freebase within several hopes of it (freebase api is used). Directed relations and properties of nodes are used as features (binary again). Also, P(relation|question) is learned and top-k is used as features. Finally, features for question and freebase node are concatenated in pairs.
To compute p(relation|question) authors assumed independence of words and used Naive Bayes (p(r|q)~p(q|r)p(rel))
If relation is complicated and hasn't been seen, constituents are used as back off model, e.g. people.person.parents and each of subparts. Authors extracted sentences from clueweb and used clueweb Freebase annotations to extract sentences, with 2 entities which represent some relation. The same for sub relation. Finally GIZA++ translation model was used to find alignments and probabilities were computed from coocurance matrix.Experiments report improvements over results that used intermediate lambda-calculus or CCG parsing.
The system is called jacana-freebase and source code is available.
4. **J. Berant and P. Liang. Semantic parsing via paraphrasing**, ACL, 2014
This paper is the follow up work of Berand and Persi Liang, the idea is to generate lambda-logical form for a natural question. The approach the authors took is to first generate a manageable set of candidate logical forms, hopefully containing the correct form. Then from each of the logical forms generate canonical utterance (question text from logical form using Freebase relation descriptions). When canonical utterances are generated, we use the paraphrase model to score similarity between given utterance and canonical ones and choose the best logical forms according to which canonical utterances they generated. Model scores logical form (features similar to those used in the previous version of the work) and paraphrase, Paraphrase model is a combination of 2 models - coocurance of phrases in monolingual parallel corpus (WikiAnswers questions) and vector space model (based on word2vec, weight of a phrase is average of word weights).
The model is called ParaSemPre and source code is available.
5. **A. Bordes, S Chopra, and J Weston, Question Answering with Subgraph Embeddings**, ArXiv 2014
Similar to 4), but uses neural networks to embed questions and candidate answer subgraph. Uses many datasets: WebQuestions, Freebase with triples converted to questions, ClueWeb with questions from the same sentence mentions, WikiAnswers paraphrases (models tries to embed paraphrases close to each other).
6. **An Overview of Microsoft Deep QA System on Stanford WebQuestions Benchmark**, Microsoft Technical Report, 2014.
A combination of large number of subsystems... Didn't read in details, and real details are not provided in the paper anyway.
7. **Siva Reddy, Mirella Lapata, Mark Steedman, Large-scale Semantic Parsing without Question-Answer Pairs**, TACL, 2014
Finds sentences in ClueWeb with 2 related entities connected in Freebase, parses it with CCG and builds a semantic graph. Semantic graph is grounded with Freebase (replacing phrases with relations, etc). Many groundings are possible, so we try all and train a model to predict the correct one. To train, an entity from a sentence is replaced with a variable and Freebase grounded graph becomes a query. Then query returning correct result is considered good. Parameters are tuned using structured perceptron. QA is done is a similar way, CCG parses questions and grounding is performed, scoring with trained model. The top grounding produces the answer.
The paper reports result slightly better than ParaSEMPRE, but they used only small subset of questions (semantic parsing is expensive) and ParaSEMPRE without some hand-coded features.
Code is here (GraphParser): http://sivareddy.in/downloads (https://drive.google.com/folderview?id=0B70MZpgZIn7oQlpKUVR1Xy1laWc&usp=drive_web)
8. **Scaling Semantic Parsers with On-the-fly Ontology Matching** Kwiatkowski et al, EMNLP, 2013
Haven't read, but this work is prior to ParaSemPre and is outperformed. They didn't use WebQuestions dataset.
9.  Lean Question Answering over Freebase from Scratch (Yao, NAACL 2015)
Find the topical entity and predict which of up to length 2 paths is the answer using n-gram features from the question.
10. Similar to 9, but adds additional query templates, keeps all entity candidates with their scores and applies pairwise ranking training using random forest. They used features to match relation name and description from Freebase to words and n-grams in the question. This turned out to be one of the most useful features.
11.  Semantic Parsing via Staged Query Graph Generation: Question Answering with Knowledge Base (Yih et al ACL, 2015)
Similar to 9, but uses better entity linking and adds restrictions, such as another entity from the question, arg min or max over numeric value, gender, type etc. Models are also better than logistic regression, typically some kind of NN.

## Answer tagging
Another relevant line of work is answer tagging. IR based QA systems extract sentences from retrived documents and then tries to tag tokens answer/no_answer.

1. X. Yao et al. Answer Extraction as Sequence Tagging with Tree Edit Distance, NACL-HLT, 2013.

## External Data Used

- **ReVerb extracted triples from ClueWeb** (10M or something), just a subset can be linked to Freebase [SemPre]
- **ClueWeb** and its Freebase entity annotations (extract sentences containing pairs of entities related in Freebase) [GraphParser]
- **WikiAnswers** (have questions clusters grouped by users). Typically only questions are used, nobody looked on free-text answer text. Questions clusters are used to build a paraphrase model (phrase => phrase scores, etc) [ParaSempre, Paralex]
- **Freebase** - triples are converted to questions automatically and used for training by Bordes et al.

# Approach
Reimplement the AQQU system and extend it with features derived from text-based resources:
- snippets and documents retrieved by querying a web search using question, question and answer entities

# TODO
1. Scrape Bing Search API for all questions from the webquestions dataset
1. Reimplement Aqqu question answering model
    1. Build entity linking dictionary
    1. Sketch the Aqqu algorithm to reimplement
1. Reimplement AskMSR+ algorithm, which we can use as a baseline