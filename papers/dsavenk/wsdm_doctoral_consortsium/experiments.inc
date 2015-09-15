\subsection{Text-based question answering}

For factoid question answering most of the systems are evaluated on the TREC QA datasets.
The closest comparison system is a system proposed in \cite{Sun:2015:ODQ:2736277.2741651}, which uses entity descriptions and types from Freebase.

For non-factoid question answering this year TREC pioneered a new question answering track - TREC LiveQA\footnote{http://trec-liveqa.org/}, which targets questions asked by real users of Yahoo! Answers.
This year the deadline for system submission was on August 31 and my system trained on CQA QnA pairs participated in the challenge.
The results will be available on the TREC Conference in November 2015.
Organizers plan to continue with another TREC LiveQA task next year and this is going to be a good estimatation of the effectiveness of the proposed techniques on hard real user questions.

\subsection{Question answering over linked data}

Recently there's been a number of works on question answering over Freebase thanks to the publicly available benchmark dataset - WebQuestions \cite{BerantCFL13:sempre}.
The dataset represents a collection of question and correct answer entity/ies.
The questions were collected using Google Suggest API and answers crowdsourced using Amazon Mechanical Turk\footnote{http://mturk.com/}
Since the questions in the dataset come from Google search logs, it is a better approximation of real user needs and is cheaper to obtain than some previous benchmarks, e.g. Free917, which contains correct logical forms.
Since a lot of models have been evaluated on this dataset, the experiments with the proposed model should include the evaluation on the WebQuestions dataset to make a direct comparison against the previous research.

However, this dataset has its certain limitations:
\begin{itemize}
\item limited variability of the questions and their lexicon. As an artifact of using Google Suggest API as a data source, many questions from the dataset use very similar lexicon and question structure to ask for some specific information.
\item limited variability of correct logical forms. The correct answers to the questions were labeled using the entity profile pages on the freebase website, which contain only entities connected directly or through a mediator node. Therefore most of the state-of-the-art results on the dataset use a small number of predefined logical form patterns.
\end{itemize}

On the other hand CQA websites have a fraction of factoid questions with provided text answers.
\cite{SavenkovLDA15} used such data for relation extraction for knowledge base completion.
I propose to use a similar way to construct a new dataset for question answering over Freebase by selecting a subset of QnA pairs with at least one entity in question and answer and some reasonable filtering heuristics and manual validation using crowdsourcing (e.g. thorugh Amazon Mechanical Turk\footnote{http://www.mturk.com}).