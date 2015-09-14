Why question answering is an interesting research direction.

Need to mention Siri, Cortana, Wolfram Alpha, etc.

\begin{figure*}
\centering
\includegraphics[width=0.5\textwidth]{img/qa_architecture}
\caption{Architecture of typical QA system}
\label{fig:qa_architecture}
\end{figure*}

Modern text-based QA systems typically share the same architecture (Figure \ref{fig:qa_architecture} {\textbf{REPLACE PICTURE WITH MY OWN}) and differ only in how each block is implemented \cite{Kolomiyets:2011:SQA:2046840.2047162}.

----
A typical systems first retrieves a set of documents that might contain the answer and then extract, score and rank candidate phrases or passages.
A number of different signals are used for candidate scoring, i.e. popularity of the candidate in the retrieved set of documents, type of the candidate entity, correspondence of the sentence language to the expected language model, etc.
Many systems doesn't consider any information about the candidate besides whats provided in the retrieved documents.
However, external resources, such as Wikipedia, were shown to be useful not only for candidate generation \cite{ahn2005using}, but also for answer validation \cite{buscaldi2006mining}.
---
Recently \cite{Sun:2015:ODQ:2736277.2741651} proposed to use Freebase, a large scale open-domain knowledge base, for answer filtering and scoring.
More specifically, authors proposed to use Freebase entity types and entity textual description. 
Freebase entity types are helpful for scoring the ``appropriateness'' of an entity as an answer to the given question, and textual description provides a brief summary of entity, which can be matched against the question for validation.
However, information about relations of the entity with other entities stored in the knowledge graph can also be helpful.
For example, below is one of the questions from TREC QA 2007:\\
\textit{What republican senators supported the nomination of Harriet Miers to Supreme Court?}\\
Freebase doesn't have a type ``republican senator'', but there is a predicate /government/politician/party, which can help a QA system to score correct and filter out incorrect candidates.
Additionally, the presence of certain relations between entities mentions in the question and candidate answer can be very useful scoring signal, given a TREC QA 2007 question \textit{For which newspaper does Krugman write?} a candidate entity \textit{New York Times} is related to entity Paul Krugman in Freebase and can be ranked higher than some other candidate newspapers.

-----

Schema-based knowledge bases, such as Freebase and dbPedia, provide an effective way to work with stored information using structured queries, such as SPARQL.
However, regular people don't know such special languages and would prefer to use natural language for querying.
Unfortunately, the structure of knowledge bases lack much lexical information and a system needs to learn to connect natural language phrases to objects in the knowledge base.
To learn such lexicon modern KB questions answering systems (e.g. \cite{yao-scratch-qa-naacl2015}, \cite{bastmore:cikm:2015:aquu}, \cite{yih:ACL:2015:STAGG}) use such resources, as question paraphrases \cite{BerantL14:parasempre}, Wikipedia \textbf{CITE} and relation phrases obtained using distant supervision \textbf{CITE}.
However, the main data source is the provided set of training question-answer entity pairs.
As a result systems learn to answer questions, similar to those seen during training.

\cite{ReddyLS14} proposed to train a question answering system from sentences in a text corpus containing KB entities and not using any training question-answer corpora.
Their system uses CCG parser to convert a sentence into a graph, which is then grounded to Freebase and converted into a KB query.
The advantage of this approach is the ability to construct compositional queries, which do not rely on the provided sample question-answer pairs.
However, the model in \cite{ReddyLS14} depend on the CCG parse of a sentence and makes an assumption that edges of the produced graph can be grounded to Freebase types and predicates, which is not always the case.
In addition, the model assumes there is no lexical gap between question and answer text.


It was demonstrated that on general QA datasets text-based question answering systems have better performance than pure knowledge-based systems \cite{Sun:2015:ODQ:2736277.2741651}, because lexical information, which is present in various documents mentioning the same factual information is missing from knowledge bases.