The ability to answer user questions with precise and concise information is a hard problem with a long history of research.
Recently we witnesses some successes of QA systems, i.e. IBM Watson winning the Jeopardy! TV show, major companies adapting question answering technologies (Apple Siri, Google Now, Microsoft Cortana, etc). 
However, these systems are still very limited and we have a lot to do to move beyond these 10 blue links in search results \cite{etzioni2011search} as for most of the questions users still have to dig into the retrieved documents or post questions to the community question answering (CQA) websites.
One of the main challenges for question answering systems is \textbf{DESCRIBE THE GLOBAL PROBLEM}.

There are two main approaches to question answering: question over unstructured text collections, and over structured knowledge bases (e.g. dpPedia, Freebase, etc. \cite{auer2007dbpedia,Bollacker:2008:FCC:1376616.1376746}).

Text-bases QA systems were shown to be quite effective on the TREC QA tasks as well as on other benchmark datasets \cite{dang2007overview}.
These systems first retrieve a set of documents using the question or some transformation of the question as the query \cite{AgichteinLG01}, and then extract entities, phrases, sentences or paragraphs believed to be the answer to the question.
The redundancy of the information in large collections makes it easier to match the question against paragraphs using language similar to those in the question \cite{LinK03}.
However, the information available in the retrived pieces of text is very limited and recent works demonstrated that usign additional semantic data, such as Wikipedia, is very useful for candidate generation \cite{ahn2005using} as well as answer validation \cite{buscaldi2006mining}.
Knowledge bases were also shown to be effective, for example in \cite{Sun:2015:ODQ:2736277.2741651} authors proposed to find Freebase entities mentioned in the answer and use their textual description and types as additional filtering and ranking signals.
However, the description and types represent a very limited view of the information available in the KB.
For example, below is one of the questions from TREC QA 2007:\\
\textit{``What republican senators supported the nomination of Harriet Miers to the Supreme Court?''}\\
A candidate answer sentence \textit{``Minority Leader Harry Reid had already offered his open support for Miers.''} mentions a senator ``Harry Reid'' and clearly says about his support of the nomination.
However, ``Harry Reid'' is not a correct answer to the question because he is a member of the Democratic party.
This information is not available in the answer candidate sentence, but it is present as one of the entity properties: [Harry Reid, political\_party, Democratic party]\footnote{Actually, in Freebase the entities are connected by a path of length 2 through a mediator node. The predicates on the path are: /government/politician/party and /government/political\_party\_tenure/party}.

Question answering over linked data (knowledge bases) converts a natural language question into a structured query, such as SPARQL.
The main challenge for such systems is to map words and phrases from the question to the corresponding entities and predicates from a KB.
Usually, such lexicon is built during training using ground truth question-query pairs \cite{CaiY13} or question-answer pairs \cite{BerantCFL13:sempre}.
Improvements were made by extending the lexicon using Wikipedia and patterns expressing certain predicated obtained via distant supervision \cite{BordesCW14:emnlp,YaoD14,ReddyLS14,bastmore:cikm:2015:aquu,yih:ACL:2015:STAGG}.
However, the systems are still biased towards questions present in the training set, which is usually not too big, and therefore do not generalize well to unseen types of questions.
It was demonstrated that on general QA datasets text-based question answering systems have better performance than pure knowledge-based systems \cite{Sun:2015:ODQ:2736277.2741651}.
Therefore, there is a big potential in using more available unstructured text data for training and testing of question answering systems operating over knowledge bases.

Some systems take a hybrid approach to question answering and generate candidate answers from multiple various data sources, e.g. \cite{baudivs2015modeling,Ferrucci10:DeepQA}.
However, these systems typically extract candidates from each of the data sources separately and only merge them on the scoring and ranking stage when some information is already lost.
This work proposes to joint the data sources together during the candidate generation stage, which should help to produce a better candidate set.

%\begin{figure*}
%\centering
%\includegraphics[width=0.5\textwidth]{img/qa_architecture}
%\caption{Architecture of typical QA system}
%\label{fig:qa_architecture}
%\end{figure*}

