The field of question answering has a long history of research and dates back to 60s, when first systems attempted to provide a natural language interface to databases \cite{Simmons:1965:AEQ:363707.363732}.
In 70s and 80s the development of restricted domain knowledge bases set a task for question answering frameworks to assist users in solving their problem, which lead to the development of interactive question answering systems, e.g. \cite{shortliffe1976mycin}, \cite{woods1977lunar}.
The modern era of question answering research started with the rise of the Internet and exponential growth of information available in the World Wide Web.
Since 1999 the annual Text Retrieval Conference (TREC)\footnote{http://trec.nist.gov} organized a number of open domain question answering shared tasks, e.g. see \cite{dang2007overview} for a review.
In 2015 TREC piloted a LiveQA track\footnote{http://trec-liveqa.org/}, in which the participant systems had to answer various questions coming from real users of Yahoo! Answers\footnote{http://answers.yahoo.com/} in real time.
A reader can refer to available surveys (e.g. \cite{Kolomiyets:2011:SQA:2046840.2047162}) for more details on the development of the field over the year of research.

The following two sections will describe research in two major types of question answering systems: question answering over text document collections and over structured knowledge bases.

\subsection{Unstructured data QA}

A traditional approach to question answering over document collections popularized by the TREC QA competition in the late 90s is to retrieve documents expected to contain the answer to the given question and extract and rank mentioned entities as candidate answers.
Words in a document containing the answer often differs from words used in the question text. 
Various approaches have been proposed to address this challenge, i.e. answer patterns construction \cite{SchlaeferGSW06} and machine translation models \cite{RiezlerVTML07}.
Another challenge lies in matching between the expected type of the answer and the retrieved candidates.
The systems relied heavily on special ontologies that encode the relationships between different question and answer types, e.g. \cite{LiRoth02, hovy2000question, prager2006question}.
Such ontologies were complicated and hard to build.

Later, the AskMSR system of \cite{brill_askmsr} used the redundancy of large text collections such as the web to extract n-grams that occur frequently in a retrieved set of documents.
In recent work \cite{tsai2015web} the system was reevaluated with the current web search, which has apparently improved since the original model was proposed.
Their counting-based approach performed unexpectedly well on TREC 2001 and sparkled an interest in exploring the web for question answering purposes \cite{LinK03}.

Another challenge lies in the fact that text-based QA system needs to make a decision based on very limited amount of information retrieved from the text collection.
External unstructured (e.g. Wikipedia articles  \cite{ahn2005using, buscaldi2006mining}) and structured data sources (e.g. Wordnet \cite{pasca2001informative}) have been shown to boost the performance of QA systems.
Recently \cite{Sun:2015:ODQ:2736277.2741651} proposed to use Freebase, a large scale open-domain knowledge base, for answer filtering and scoring.
More specifically, authors proposed to use Freebase entity types and entity textual description.
Freebase entity types are helpful for scoring the ``appropriateness'' of an entity as an answer to the given question, and textual description provides a brief summary of entity, which can be matched against the question for validation.
However, description and types don't include all the information available in the KB and it is potentially beneficial to use the relations between entities as well.

\subsection{Structured data QA}

Early knowledge bases were mainly domain specific and so were the corresponding question answering systems.
Recent development of large scale knowledge bases (e.g. dbPedia \cite{auer2007dbpedia}) and Freebase \cite{Bollacker:2008:FCC:1376616.1376746}) motivated research in open domain question answering over linked data.
Developed models can be compared on annual QALD shared task \footnote{http://greententacle.techfak.uni-bielefeld.de/~cunger/qald/} and on a number of available benchmark datasets, among which one of the most popular dataset is called WebQuestions \cite{BerantCFL13:sempre}.
In the last couple of years many different approaches were tested on the later dataset, and can be classified into information extraction and semantic parsing based approaches based on whether the system generates and ranks a set of candidates first or builds an accurate semantic parse from the beginning.
A number of different lexical resources have been used, e.g. alignment of Freebase to a large text collection such as ClueWeb \cite{BerantCFL13:sempre, YaoD14, ReddyLS14}, question paraphrases clusters from WikiAnswers \cite{BerantL14:parasempre}, Freebase triples rephrased as questions \cite{BordesCW14:emnlp}.
In some approaches embeddings of text and/or knowledge base predicates are used instead of word and phrase matches \cite{BordesCW14:emnlp,yih:ACL:2015:STAGG).

However, most of the models are trained on the QnA pairs from the WebQuestions dataset and are somewhat biased towards the questions patterns present in the training set.
Alternatively, \cite{ReddyLS14} proposed to train a question answering system from sentences in a text corpus containing KB entities and not using any training question-answer corpora.
Their system uses CCG parser to convert a sentence into a graph, which is then grounded to Freebase and converted into a KB query.
The advantage of this approach is the ability to construct compositional queries, which do not rely on the provided sample QnA pairs.
However, the model in \cite{ReddyLS14} depend on the CCG parse of a sentence and makes an assumption that edges of the produced graph can be grounded to Freebase types and predicates, which is not always the case.
In addition, the model assumes there is no lexical gap between question and answer text.

This work proposes to use QnA pairs available on CQA websites to extend the set of questions a system sees during training.
Secondly, I propose to use lexical resources not only during training but also while application of the QA model as an additional signal for candidate knowledge base queries ranking.

\subsection{Hybrid techniques}

Hybrid question answering systems combine multiple available information sources, in particular text document and knowledge bases.
Examples of such systems include IBM Watson \cite{Ferrucci10:DeepQA}, OpenQA \cite{Fader:2014:OQA:2623330.2623677}, YodaQA\cite{baudivs2015modeling}.
The main difference between the proposed research and hybrid QA systems is that such models use separate pipelines to extract candidate answers from text, knowledge bases, etc. and only combine the information on the ranking stage.
This work argues that it is beneficial to extend the representation of each of the data sources while generating candidates to improve the overall performance.
