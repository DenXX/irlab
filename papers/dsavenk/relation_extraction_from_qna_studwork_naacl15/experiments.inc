\subsection{Datasets}

For experiments we used 2 publicly available CQA datasets: Yahoo! Answers Comprehensive Questions and Answers\footnote{http://webscope.sandbox.yahoo.com/catalog.php?datatype=l} and a crawl of WikiAnswers\footnote{http://wiki.answers.com} \cite{Fader:2014:OQA:2623330.2623677}.
The Yahoo! Answers dataset contains 4,483,032 questions (3,894,644 in English) with the corresponding answers collected on 10/25/2007.
The crawl of WikiAnswers has 30,370,994 question clusters, tagged by WikiAnswers users as paraphrases, and only 3,386,256 them have an answer.
From these clusters we used all possible pairs of questions and answers (19,629,443 pairs in total).

\begin{table*}[ht]
\centering
\caption{Yahoo! Answers and WikiAnswers datasets statistics}
\vspace{-2mm}
\label{table:cqastats}
\begin{tabular}{|p{12.5cm}||p{1.2cm}|p{1.2cm}|} \hline
& Y!A & WA\\
\hline
Number of QnA pairs & 3.8M & 19.6M \\
Average question length (in chars) & 56.67 & 47.03 \\
Average answer length (in chars) & 335.82 & 24.24 \\
% Number of resolved entities per QnA pair & 3.57 & 3.23 \\
Percent of QnA pairs with answers that do not have any verbs & 8.8\% & 18.9\% \\
Percent of QnA pairs with at least one pair of entities related in Freebase & 11.7\% & 27.5\% \\
Percent of relations between entity pairs in question sentences only & 1.6 \% & 3.1\% \\
Percent of relations between entity pairs in question and answer sentences only & 28.1\% & 46.4\% \\
Percent of relations between entity pairs in answer sentences only & 38.6\%& 12.0\%\\
\hline
\end{tabular}
\end{table*}

\subsection{Preprocessing}

For each concatenated QnA pair we applied tokenization, sentence detection, named entity tagger, parsing and coreference resolution from Stanford CoreNLP \cite{manning-EtAl:2014:P14-5}.
We used an entity linking approach similar to \newcite{chang2011stanford} and considered all noun phrase and named entity mentions as candidates.
Our entity linking cascade has 4 stages, 2 of which applies to named entity mentions only.
At the first stage all named entity mentions are looked up in Freebase names and aliases dictionary.
The next two stages attempt to match mention text with dictionary of English Wikipedia concepts \cite{spitkovsky2012cross} and its normalized version.
Finally for named entity mentions we try spelling correction using Freebase entity names dictionary.
We didn't disambiguate entities and instead took top-5 ids for each coreference cluster (using the $p(entity|phrase)$ score from the dictionary or number of existing Freebase triples).

All pairs of entities (or entity and date) in a QnA pair that are directly related\footnote{We also consider some paths that come through a mediator node, \eg  /people/person/spouse\_s./people/marriage/spouse} in Freebase were annotated with the corresponding relations.

Table \ref{table:cqastats} gives some statistics on the datasets used in this work.
The analysis of answers that do not have any verbs show that $\sim$8.8\% of all QnA pairs do not state the predicate in the answer text.
The percentage is higher for WikiAnswers, which has shorter answers on average.
Unfortunately, for many QnA pairs we were unable to find relations between the mentioned entities (for many of them no or few entities were resolved to Freebase).
Among those QnA pairs, where some relation was annotated, we looked at the location of related entities.
In Yahoo! Answers dataset answers are long and 38.6\% of relations occur in answer sentences, and 28.1\% between entities mentioned in question and answer sentences.
For WikiAnswers we can see that more relations (46.4\%) occur between entities mentioned in different sentences.
% Such relations are not considered by existing techniques and our goal is to extract some of them.

\subsection{Experimental setup}

For our experiments we use a subset of 29 Freebase predicates that have enough unique instances annotated in our corpus, \eg
date\_of\_birth, profession, nationality, education institution, date\_of\_death, disease treatments, disease symptoms, book author, music artist album, \etc
We train and test the models on each dataset separately.
Each corpus is randomly split for training (75\%) and testing (25\%).
Knowledge base facts are also split into training and testing sets (50\% each).
QnA and sentence-based models predict labels for each entity pair mention, and we aggregate mention predictions by taking the maximum score for each predicate.
We do the same aggregation to produce a combination of QnA- and sentence-based models, \ie, all extractions produced by the models are combined and if there are multiple extractions of the same fact we take the maximum score as the final confidence.
The precision and recall of extractions are evaluated on a test set of Freebase triples, \ie an extracted triple is considered correct if it belongs to the test set of Freebase triples, which are not used for training (triples used for training are simply ignored).
Note, that this only provides a lower bound on the model performance as some of the predicted facts can be correct and simply missing in Freebase.

\subsection{Results}

Figure \ref{figure:pr} shows Precision-Recall curves for QnA-based and sentence-based baseline models and some numeric results are given in Table \ref{table:results}.
As 100\% recall we took all pairs of entities that can be extracted by either model.
It is important to note, that since some entity pairs occur exclusively inside the answer sentences and some in pairs of question and answer sentences, none of the individual models is capable of achieving 100\% recall, and maximum possible recalls for QnA- and sentence-based models are different.

\begin{figure}[h!]
\centering
\vspace{-2mm}
\begin{subfigure}[h]{0.45\textwidth}
	\includegraphics[width=0.99\textwidth]{img/qa_vs_sent_ya}
	\vspace{-1mm}
    \label{figure:pr:ya}
\end{subfigure}
\begin{subfigure}[h]{0.45\textwidth}
	\includegraphics[width=0.99\textwidth]{img/qa_vs_sent_wa}
	\vspace{-1mm}
    \label{figure:pr:wa}
\end{subfigure}
\begin{subfigure}[h]{0.45\textwidth}
	\includegraphics[width=0.99\textwidth]{img/noqf_vs_qf}
	\vspace{-1mm}
	\label{figure:pr:noqf_vs_qf}
\end{subfigure}
\vspace{-1mm}
\caption{Precision-Recall curves for QnA-based vs sentence-based models and sentence-based model with and without question features}
\label{figure:pr}
\end{figure}

\begin{table*}[tbh]
\centering
\caption{Extraction results for QnA- and sentence-based models on both datasets}
\vspace{-2mm}
\label{table:results}
\begin{tabular}{|p{6.6cm}||p{0.8cm}|p{1.2cm}|p{1.4cm}||p{0.8cm}|p{1.2cm}|p{1.4cm}|}
\hline
& \multicolumn{3}{|c||}{Yahoo! Answers} & \multicolumn{3}{|c|}{WikiAnswers}\\
\cline{2-7}
& QnA & Sentence & Combined & QnA & Sentence & Combined\\
\hline
F-1 score & 0.219 & 0.276 & 0.310 & 0.277 & 0.297 & 0.332\\
Number of correct extractions & 3229 & 5900 & 7428 & 2804 & 2288 & 3779 \\
Correct triples not extracted by other model & 20.5\% & 56.5\% & - & 39.4\% & 25.8\% & - \\
\hline
\end{tabular}
\end{table*}

Results demonstrate that from 20.5\% to 39.4\% of correct triples extracted by the QnA-based model are not extracted by the baseline model, and the combination of both models is able to achieve higher precision and recall.
Unfortunately, comparison of sentence-based model with and without question-based features (Figure \ref{figure:pr}) didn't show a significant difference.

%\begin{figure*}[ht]
%\centering
%\begin{subfigure}[t]{0.45\textwidth}
%	\includegraphics[width=\textwidth]{img/pr_dob}
%	\caption{person.person.date\_of\_birth predicate}
%    \label{figure:pr_comp:dob}
%\end{subfigure}
%\begin{subfigure}[t]{0.45\textwidth}
%	\includegraphics[width=\textwidth]{img/pr_symptoms}
%	\caption{medicine.decease.symptoms predicate}
%    \label{figure:pr_comp:symptoms}
%\end{subfigure}
%\caption{Precision-Recall curves of QnA and sentence-based relation extraction models for two of the predicates}
%\label{figure:pr_comp}
%\end{figure*}
