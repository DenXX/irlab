% Recently we witnessed some successes of QA systems, i.e. IBM Watson winning the Jeopardy! TV show, major companies adapting question answering technologies (Apple Siri, Google Now, Microsoft Cortana, etc). 
% However, these systems are still very limited and we have a lot to do to move beyond these 10 blue links in search results \cite{etzioni2011search} as for most of the questions users still have to dig into the retrieved documents or post questions to the community question answering (CQA) websites.

The ability to answer user questions with precise and concise information is a hard problem with a long history of research.
Various data sources are available for candidate answer generation, two major ones are unstructured text corpora, and structured knowledge bases (e.g. dpPedia \cite{auer2007dbpedia} and Freebase \cite{Bollacker:2008:FCC:1376616.1376746}).
A hybrid approach to question answering \cite{baudivs2015modeling,Ferrucci10:DeepQA} generates candidates from multiple sources, however each of them is typically processed separately and results are merged on the scoring and ranking stage when some information is already lost.
Efficient combination of different information sources has potential to improve both text and knowledge base question answering systems.
I propose to combine all the available sources together and do joint reasoning to generate better answer candidates and improve the overall question answering performance.

% Text-bases QA systems were shown to be quite effective on the TREC QA tasks as well as on other benchmark datasets \cite{dang2007overview}.
Question answering from text corpora typically starts by retrieving a set of potentially relevant documents using the question (or some transformation of the question \cite{AgichteinLG01}) as the query, and then extracting entities, phrases, sentences or paragraphs believed to be the answer to the question.
However, the information available in the retrieved pieces of text is very limited and often not enough to decide whether it can be the answer to the given question.
For example, below is one of the questions from TREC QA 2007 dataset:\\
\textit{``What republican senators supported the nomination of Harriet Miers to the Supreme Court?''}\\
A candidate answer sentence \textit{``Minority Leader Harry Reid had already offered his open support for Miers.''} mentions a senator ``Harry Reid'' and clearly says about his support of the nomination.
However, ``Harry Reid'' is not a correct answer to the question because he is a member of the Democratic party.
This information is not available in the answer candidate sentence, but it is present as one of the properties in Freebase: [Harry Reid, political\_party, Democratic party]\footnote{Actually, in Freebase the entities are connected by a path of length 2 through a mediator node. The predicates on the path are: /government/politician/party and /government/political\_party\_tenure/party}.
Therefore, by looking into the knowledge available about the mentioned entities a QA system can make a better judgment about the candidate answer.

Question answering over linked data (knowledge bases) converts a natural language question into a structured query, such as SPARQL.
The main challenge for such systems is to map words and phrases from the question to the corresponding entities and predicates from a KB.
Usually, such lexicon is built during training using ground truth question-query pairs \cite{CaiY13} or question-answer pairs \cite{BerantCFL13:sempre}.
Improvements were made by extending the lexicon using Wikipedia and patterns expressing certain predicates obtained via distant supervision \cite{bastmore:cikm:2015:aquu,BordesCW14:emnlp,ReddyLS14,yih:ACL:2015:STAGG,YaoD14}.
But still, the amount of available labeled or weakly labeled training data is much smaller than the amount of unstructured data.
This unstructured data will complement the learned lexicon, e.g. even if a question about a certain predicate wasn't seen during training, a set of text paragraphs mentioning both of the related entities can provide a QA system with enough evidence to make the correct decision.

% In addition, these text paragraphs can be presented to the user to support the returned answer if needed.

%\begin{figure*}
%\centering
%\includegraphics[width=0.5\textwidth]{img/qa_architecture}
%\caption{Architecture of typical QA system}
%\label{fig:qa_architecture}
%\end{figure*}

