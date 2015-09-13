
% I want to say, that systems either use text to get candidate answers, or they use knowledge bases. Hybrid systems usually have independent pipelines to get all candidates and then merge.
Most of the existing systems for factoid question answering are based either on unstructured natural language text collections or structured knowledge bases, or they take a hybrid approach and merge answer candidates obtained from multiple sources.

\textbf{DESCRIBE THE IDEA OF THE PROPOSAL IN A SENTENCE OR TWO}
I propose to use all available data sources jointly....
CQA and the web and KB. Combine them!!!

\subsection{Text-based question answering}

\begin{figure*}
\centering
\epsfig{file=qa_architecture}
\caption{Architecture of typical QA system}
\label{fig:qa_architecture}
\end{figure*}

Modern text-based QA systems typically share the same architecture (Figure \ref{fig:qa_architecture} {\textbf{REPLACE PICTURE WITH MY OWN}) and differ only in how each block is implemented \cite{Kolomiyets:2011:SQA:2046840.2047162}.
A typical systems first retrieves a set of documents that might contain the answer and then extract, score and rank candidate phrases or passages.
A number of different signals are used for candidate scoring, i.e. popularity of the candidate in the retrieved set of documents, type of the candidate entity, correspondence of the sentence language to the expected language model, etc.
Many systems doesn't consider any information about the candidate besides whats provided in the retrieved documents.
However, external resources, such as Wikipedia, were shown to be useful not only for candidate generation \cite{ahn2005using}, but also for answer validation \cite{buscaldi2006mining}.

\subsubsection{Using Knowledge Base for text-based QA}

Recently \cite{Sun:2015:ODQ:2736277.2741651} proposed to use Freebase, a large scale open-domain knowledge base, for answer filtering and scoring.
More specifically, authors proposed to use Freebase entity types and entity textual description. 
Freebase entity types are helpful for scoring the ``appropriateness'' of an entity as an answer to the given question, and textual description provides a brief summary of entity, which can be matched against the question for validation.
However, information about relations of the entity with other entities stored in the knowledge graph can also be helpful.
For example, below is one of the questions from TREC QA 2007:\\
\textit{What republican senators supported the nomination of Harriet Miers to Supreme Court?}\\
Freebase doesn't have a type ``republican senator'', but there is a predicate /government/politician/party, which can help a QA system to score correct and filter out incorrect candidates.
Additionally, the presence of certain relations between entities mentions in the question and candidate answer can be very useful scoring signal, given a TREC QA 2007 question \textit{For which newspaper does Krugman write?} a candidate entity \textit{New York Times} is related to entity Paul Krugman in Freebase and can be ranked higher than some other candidate newspapers.

\textbf{DETAILS ON WHAT DO I PROPOSE TO DO HERE...}

\subsubsection{Using CQA data for text-based QA}
Question and answer text doesn't always use the same words.
Machine translation techniques are often used to bridge this language gap.
To train such systems we need a lot of data, which can be found in community question answering websites.
\textbf{BTW, I'M NOT SURE ABOUT RELATED WORK HERE, NEED TO CHECK}.
This year TREC pioneered a new question answering track - TREC LiveQA\footnote{http://trec-liveqa.org/}, which targets questions asked by real users of Yahoo! Answers.
I participated in the challenge\footnote{Results will be available on the TREC Conference in November 2015} and developed a system, that learns to rank best answers to a given questions versus answer to other similar questions.
\textbf{SOMETHING ELSE SHOULD FOLLOW}

\subsection{Question Answering over linked data}
\subsubsection{Dataset}
Recently there's been a number of works on question answering over Freebase thanks to the publicly available benchmark dataset - WebQuestions \cite{BerantCFL13:sempre}.
The dataset represents a collection of question and correct answer entity/ies.
The questions were collected using Google Suggest API and answers crowdsourced using Amazon Mechanical Turk\footnote{http://mturk.com/}
Since the questions in the dataset come from Google search logs, it is a better approximation of real user needs and is cheaper to obtain than some previous benchmarks, e.g. Free917, which contains correct logical forms.
However, this WebQuestions dataset has its own limitations:
\begin{itemize}
\item limited variability of the questions and their lexicon. As an artifact of using Google Suggest API as a data source, many questions from the dataset use very similar lexicon and question structure to ask for some specific information.
\item limited variability of correct logical forms. The correct answers to the questions were labeled using the entity profile pages on the freebase website, which contain only entities connected directly or through a mediator node. Therefore most of the state-of-the-art results on the dataset use a small number of predefined logical form patterns.
\end{itemize}

On the other hand CQA websites have a fraction of factoid questions with provided text answers.
\cite{SavenkovLDA15} used such data for relation extraction for knowledge base completion.
In a similar way we can construct a new dataset for question answering by selecting a subset of QnA pairs with at least one entity in question and answer and some reasonable filtering heuristics.

\subsubsection{Weakly supervised training}
Schema-based knowledge bases, such as Freebase and dbPedia, provide an effective way to work with stored information using structured queries, such as SPARQL.
However, regular people don't know such special languages and would prefer to use natural language for querying.
Unfortunately, the structure of knowledge bases lack much lexical information and a system needs to learn to connect natural language phrases to objects in the knowledge base.
To learn such lexicon modern KB questions answering systems (e.g. \cite{yao-scratch-qa-naacl2015}, \cite{bastmore:cikm:2015:aquu}, \cite{yih:ACL:2015:STAGG}) use such resources, as question paraphrases \cite{BerantL14:parasempre}, Wikipedia \textbf{CITE} and relation phrases obtained using distant supervision \textbf{CITE}.
However, the main data source is the provided set of training question-answer entity pairs.
As a result systems learn to answer questions, similar to those seen during training.
Table \ref{table:kbqa_example} shows an example of question answered incorrectly by a state-of-the-art system.

\begin{table}
\centering
\caption{Motivating Example for KB QA}
\begin{tabular}{|p{8cm}|} \hline
Question: who is the woman that john edwards had an affair with?\\
\hline
Provided answer: "Writer", "Politician", "Lawyer", "Attorneys in the United States"\\
\hline
Correct answer: Rielle Hunter\\
\hline
Phrase from Wikipedia: \textbf{John Edwards} had engaged in an affair with \textbf{Rielle Hunter}...\\
\hline
\end{tabular}
\label{table:kbqa_example}
\end{table}

\cite{ReddyLS14} proposed to train a question answering system from sentences in a text corpus containing KB entities and not using any training question-answer corpora.
Their system uses CCG parser to convert a sentence into a graph, which is then grounded to Freebase and converted into a KB query.
The advantage of this approach is the ability to construct compositional queries, which do not rely on the provided sample question-answer pairs.
However, the model in \cite{ReddyLS14} depend on the CCG parse of a sentence and makes an assumption that edges of the produced graph can be grounded to Freebase types and predicates, which is not always the case.
In addition, the model assumes there is no lexical gap between question and answer text.
To alleviate this problems, I propose 1). learn lexical features directly from an alignment to a KB instead of constructing a CCG parse similar to distant supervision methods for relation extraction \cite{MintzBSJ09}; 2). Apply the same technique to a dataset obtained from QnA pairs from CQA websites.

\subsubsection{Text resources as features}
It was demonstrated that on general QA datasets text-based question answering systems have better performance than pure knowledge-based systems \cite{Sun:2015:ODQ:2736277.2741651}, because lexical information, which is present in various documents mentioning the same factual information is missing from knowledge bases.
On the other hand knowledge bases have a possibility to use not only text mentioning each particular entity or set of entities (e.g. we can extract all passages that mention a given set of entities, see Table \ref{table:kbqa_example} for an example of phrase mentioning a particular Freebase triple), but also phrases used to express predicates (not only between the current entities, but aggregated over all entity pairs in a distant supervision fashion).

%Combining document and KB-based question answering:
%\begin{itemize}
%\item use more lexical information for KB-based question answering
%	\begin{itemize}
%	\item use document collection and/or web and generate more features for each structured query
%	\item borrow some ideas from relation extraction and distant supervision for KB-based QA
%	\end{itemize}
%\item use available KB information better for collection based question answering
%	\begin{itemize}
%	\item Universal schema and PRA combine knowledge graph links and text, we need to something similar to extent the document representation. This can help both factoid and non-factoid question answering.
%	\item previous research have found description in Freebase to be useful for question answering. We can try to add answer validation stage and query the web with the answer and read what is it about, which should validate it as the answer candidate. Probably this needs to be done in combination with the question, as for example the data isn't very productive
%	\end{itemize}
%\end{itemize}