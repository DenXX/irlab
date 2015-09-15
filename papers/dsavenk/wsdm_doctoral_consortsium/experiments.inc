
\subsection{Text-based question answering}
For text-based question answering we propose to use existing benchmark dataset, such as TREC QA.


\subsection{TREC LiveQA}
This year TREC pioneered a new question answering track - TREC LiveQA\footnote{http://trec-liveqa.org/}, which targets questions asked by real users of Yahoo! Answers.
I participated in the challenge\footnote{Results will be available on the TREC Conference in November 2015} and developed a system, that learns to rank best answers to a given questions versus answer to other similar questions.

\subsection{Question answering over linked data}

Blah-blah-blah WebQuestions...

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