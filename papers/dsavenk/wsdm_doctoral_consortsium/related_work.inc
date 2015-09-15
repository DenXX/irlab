The field of question answering has a long history of research and dates back to 60s, when first systems attempted to provide a natural language interface to databases \cite{Simmons:1965:AEQ:363707.363732}.
In 70s and 80s the development of restricted domain knowledge bases set a task for question answering frameworks to assist users in solving their problem, which lead to the development of interactive question answering systems, e.g. \cite{shortliffe1976mycin}, \cite{woods1977lunar}.
The modern era of question answering research started with the rise of the Internet and exponential growth of information available in the World Wide Web.
Since 1999 the annual Text Retrieval Conference (TREC)\footnote{http://trec.nist.gov} organized a number of open domain question answering shared tasks, e.g. see \cite{dang2007overview} for a review.
In 2015 TREC piloted a LiveQA track\footnote{http://trec-liveqa.org/}, in which the participant systems had to answer various questions coming from real users of Yahoo! Answers\footnote{http://answers.yahoo.com/} in real time.
A reader can refer to available surveys (e.g. \cite{Kolomiyets:2011:SQA:2046840.2047162}) for more details on the development of the field over the year of research.

The following two sections will describe research in two major types of question answering systems: question answering over text document collections and over structured knowledge bases.

\subsection{Unstructured data QA}

A traditional approach to question answering over document collections popularized by the TREC QA competition in the late 90s is retrieve documents expected to contain the answer to the given question and extract and rank entities as answer candidates.
The systems relied heavily on special ontologies that encode the relationships between different question and answer types, e.g. \cite{hovy2000question}, \cite{prager2006question}.
Another issue for question answering over text collection is difference in the language used in the answers compared to the language of the questions.
Multiple approaches have been proposed to address this issue, e.g. learning answer patterns \cite{SchlaeferGSW06} or question to answer machine translation models \cite{RiezlerVTML07}.
Later, the AskMSR system of \cite{brill_askmsr} used the redundancy of large text collections such as the web to extract n-grams that occur frequently in a retrieved set of documents.
Their counting-based approach performed unexpectedly well on TREC 2001 and sparkled an interest in exploring the web for question answering purposes \cite{LinK03}.

Review first collection algorithms.
Review AskMSR, AskMSR+ and redundancy-based techniques.

Recently \cite{Sun:2015:ODQ:2736277.2741651} proposed to use Freebase, a large scale open-domain knowledge base, for answer filtering and scoring.
More specifically, authors proposed to use Freebase entity types and entity textual description. 
Freebase entity types are helpful for scoring the ``appropriateness'' of an entity as an answer to the given question, and textual description provides a brief summary of entity, which can be matched against the question for validation.
However, information about relations of the entity with other entities stored in the knowledge graph can also be helpful.
For example, below is one of the questions from TREC QA 2007:\\
\textit{What republican senators supported the nomination of Harriet Miers to Supreme Court?}\\
Freebase doesn't have a type ``republican senator'', but there is a predicate /government/politician/party, which can help a QA system to score correct and filter out incorrect candidates.
Additionally, the presence of certain relations between entities mentions in the question and candidate answer can be very useful scoring signal, given a TREC QA 2007 question \textit{For which newspaper does Krugman write?} a candidate entity \textit{New York Times} is related to entity Paul Krugman in Freebase and can be ranked higher than some other candidate newspapers.

\subsection{Structured data QA}
Review QALD and work on webquestions dataset.

Data available in structured form, such as knowledge bases is also a useful source for answers to user questions.
Today a number of developed large-scale open domain knowledge bases (e.g. such as Freebase \cite{Bollacker:2008:FCC:1376616.1376746}, dbPedia\cite{auer2007dbpedia}, NELL \cite{carlson2010toward}) allows to use them for answer selection.
Question answering over linked data

WebQuestions \cite{BerantCFL13:sempre}, \cite{YaoD14}, \cite{BerantL14:parasempre}, \cite{ReddyLS14}, \cite{BordesCW14:emnlp}, \cite{yao-scratch-qa-naacl2015}, \cite{bastmore:cikm:2015:aquu}, \cite{yih:ACL:2015:STAGG}


\cite{ReddyLS14} proposed to train a question answering system from sentences in a text corpus containing KB entities and not using any training question-answer corpora.
Their system uses CCG parser to convert a sentence into a graph, which is then grounded to Freebase and converted into a KB query.
The advantage of this approach is the ability to construct compositional queries, which do not rely on the provided sample question-answer pairs.
However, the model in \cite{ReddyLS14} depend on the CCG parse of a sentence and makes an assumption that edges of the produced graph can be grounded to Freebase types and predicates, which is not always the case.
In addition, the model assumes there is no lexical gap between question and answer text.


\subsection{Hybrid techniques}
Describe IBM WATSON, OPENQA, YODAQA.

Another interesting approach is based on the idea of Open Information Extraction \cite{Fader:2014:OQA:2623330.2623677}...


OpenQA is an interesting technique as it tries to convert text into OpenIE set of triples and then query it.

Describe recent work that used entity linking to Freebase.

\subsection{Reading Comprehension}
McTest dataset. Maybe this part is not relevant.
However, merging reading comprehesion to regular QA seems natural and interesting.