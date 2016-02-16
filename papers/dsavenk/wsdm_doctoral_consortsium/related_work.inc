% The field of question answering has a long history of research and dates back to 60s, when first systems attempted to provide a natural language interface to databases \cite{Simmons:1965:AEQ:363707.363732}.
% In 70s and 80s the development of restricted domain knowledge bases set a task for question answering frameworks to assist users in solving their problem, which lead to the development of interactive question answering systems, e.g. \cite{shortliffe1976mycin}, \cite{woods1977lunar}.

% The modern era of question answering research started with the rise of the Internet and exponential growth of information available in the World Wide Web.
% Since 1999 the annual Text Retrieval Conference (TREC)\footnote{http://trec.nist.gov} organized a number of open domain question answering shared tasks, e.g. see \cite{dang2007overview} for a review.
% In 2015 TREC piloted a LiveQA track\footnote{http://trec-liveqa.org/}, in which the participant systems had to answer various questions coming from real users of Yahoo! Answers\footnote{http://answers.yahoo.com/} in real time.

The field of question answering has a long history of research and dates back to 60s (see \cite{Kolomiyets:2011:SQA:2046840.2047162} for a survey of different approaches).
The modern era of question answering research started with the rise of the Internet and exponential growth of information available in the World Wide Web.
Since 1999 the annual TREC organized a number of open domain question answering shared tasks \cite{dang2007overview}.
Approaches proposed over the years can be largely classified by the type of the information used to find the answers into knowledge base and text-based systems.

\subsection{Factoid QA}

\textbf{Text-based QA}. One of the main challenges of such approaches is limited amount of information present in the extracted pieces of text.
Systems test answer for correctness by matching the expected answer type with the type of candidate entity often predicted by an named entity tagger.
These systems rely heavily on special complicated ontologies that encode the relationships between different question and answer types, e.g. \cite{hovy2000question,LiRoth02, prager2006question}.
Alternatively, the AskMSR system \cite{brill_askmsr} (recently reviewed in \cite{tsai2015web}) used the redundancy of large text collections such as the web to extract n-grams that occur frequently in a retrieved set of documents.
Their counting-based approach performed unexpectedly well on TREC 2001 and sparkled an interest in exploring the web for question answering purposes \cite{LinK03}.
However, in many cases the information from the extracted text fragments is not enough to make a judgment on an answer candidate.
To solve this problem researchers experimented with using external resources, both unstructured (e.g. Wikipedia articles  \cite{ahn2005using, buscaldi2006mining}) and structured (e.g. Wordnet \cite{pasca2001informative}), and demonstrated improved question answering performance.
Recently \cite{Sun:2015:ODQ:2736277.2741651} proposed to link entities from candidate answers to Freebase and use its type system and textual entity description for candidate scoring.
However, most of the information in a KB is stored as relations between entities, therefore there is a big potential in using all available KB data to improve question answering.

\textbf{Knowledge base QA}. Recent development of large scale knowledge bases (e.g. dbPedia \cite{auer2007dbpedia}) and Freebase \cite{Bollacker:2008:FCC:1376616.1376746}) motivated research in open domain question answering over linked data.
Developed models can be compared on the annual QALD shared task\footnote{http://greententacle.techfak.uni-bielefeld.de/$\sim$cunger/qald/} and on a number of available benchmark datasets, e.g. WebQuestions \cite{BerantCFL13:sempre}.
The main challenge of such systems is to map natural language questions into the structured query representation.
Such a lexicon can be learned from a labeled training set \cite{BerantCFL13:sempre},  ClueWeb collection aligned to Freebase \cite{ReddyLS14,YaoD14}, question paraphrases clusters from WikiAnswers \cite{BerantL14:parasempre}, Freebase triples rephrased as questions \cite{BordesCW14:emnlp}, and can be based on the embeddings of questions and knowledge base entities and predicates \cite{BordesCW14:emnlp,yih:ACL:2015:STAGG}.
However, most of the models are still biased towards the types of questions present in the training set and would benefit from more training data.
In this work I propose to extend the training set with question-answer pairs available on CQA websites, which were shown to be useful for relation extraction \cite{SavenkovLDA15}.
In addition, I propose to use unlabeled text resources for candidate query ranking, which can help to generalize to unseen types of questions and questions about predicates never mentioned in the training set.

\textbf{Hybrid techniques}. Hybrid question answering systems combine multiple available information sources, in particular text document and knowledge bases.
Examples of such systems include IBM Watson \cite{Ferrucci10:DeepQA}, OpenQA \cite{Fader:2014:OQA:2623330.2623677}, YodaQA \cite{baudivs2015modeling}.
The main difference between such systems and the proposed research is that hybrid systems typically use separate pipelines to extract candidates from different sources and only merge the candidate set while ranking.
I propose to extend the representation of each of the data sources for better candidate generation from the beginning.


\subsection{Non-factoid QA}

Research in non-factoid question answering has been mainly focused on studying features that can help to rank passages from CQA archives \cite{surdeanu2011learning,shtok2012learning} and web passages \cite{soricut2004automatic,wang2015faq}.
These approaches study various ways to bridge the lexical gap by using monolingual translation models, alignment model and other kind of question-answer similarity features.
A more structured approach for non-factoid question answering has been recently proposed in \cite{Sondhi:2014:MSO:2661829.2661968}.
Authors proposed to build a knowledge base from Wikipedia passages, and use learned SQL patterns on this KB to extract passages as answers to new questions.
In addition to Wikipedia, the approach I propose is focusing on other semi-structured web pages, such as forums, FAQ pages, etc.

Another relevant line of research is automatic extraction of question-answer pairs from web documents, e.g. \cite{Jijkoun:2005:RAF:1099554.1099571,cong2008finding} to name a few.
My research will essentially build on these techniques and integrate them into a live question answering system.
The system I'm going to build will in addition try to predict whether a passage answers the given question, which will cover web pages without explicit question-answer pairs.


