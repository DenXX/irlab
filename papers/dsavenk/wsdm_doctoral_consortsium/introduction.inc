The ability to answer user questions with precise and concise information is a hard problem with a long history of research.
There has been some recent successes of IBM Watson on the Jeopardy TV show and a wide adaptation of question answering technologies in commercial products, such as Google Now, Apple Siri, Microsoft Cortana, etc, which in some cases are able to provide users with a short answer instead of ``10 blue links''.
However, these systems is still very limited and we have a lot to do to move the area close to meet user expectaions \cite{etzioni2011search} as for most of the questions users still have to dig into the retrieved documents or post questions on a community question answering websites.

Two main directions of QA research include question answering over unstructured text collections, such as the Web, and over structured knowledge bases, such as dpPedia \cite{auer2007dbpedia}, Freebase \cite{Bollacker:2008:FCC:1376616.1376746}, etc.
There are hybrid approaches, that combine candidates retrieved from various data sources, e.g. \cite{Ferrucci10:DeepQA}, \cite{baudivs2015modeling}.
However, the pipelines in these systems are still rather independent with information combined on the ranking stage only.
Recent research suggests that enriching text with additional semantic information, such as Wikipedia, is very useful for candidate generation \cite{ahn2005using} as well as answer validation \cite{buscaldi2006mining}.
Similar to \cite{Sun:2015:ODQ:2736277.2741651} this work proposes to use a large open-domain knowledge base to enrich text with semantic annotations from a KB, and use not only entity types and textual description, but all the information available in a knowledge base.

On the other side of the spectrum question answering systems over linked data, i.e. knowledge bases, lack lexical information connecting natural language text with structured knowledge representation and query languages, such as SPARQL.
To learn such lexicon modern KB questions answering systems (e.g. \cite{yao-scratch-qa-naacl2015}, \cite{bastmore:cikm:2015:aquu}, \cite{yih:ACL:2015:STAGG}) use question paraphrases \cite{BerantL14:parasempre}, Wikipedia \cite{BordesCW14:emnlp} and relation phrases obtained using distant supervision \cite{YaoD14}.
These works showed the importance of additional textual resources for training a question answering system.
However, the main data source is the provided set of training question-answer entity pairs.
As a result systems learn to answer questions, similar to those seen during training and do not generalize well to unseen types of questions.
It was demonstrated that on general QA datasets text-based question answering systems have better performance than pure knowledge-based systems \cite{Sun:2015:ODQ:2736277.2741651}, because lexical information, which is present in various documents mentioning the same factual information is missing from knowledge bases.
Thus, there is a big potential in using more lexical resources to improve question answering over knowledge bases.

%\begin{figure*}
%\centering
%\includegraphics[width=0.5\textwidth]{img/qa_architecture}
%\caption{Architecture of typical QA system}
%\label{fig:qa_architecture}
%\end{figure*}
