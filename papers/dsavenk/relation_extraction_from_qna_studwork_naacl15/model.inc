Our models for relation extraction from QnA data incorporates the topic of the question and can be represented as a graphical model (Figure \ref{fig:graphmodel}).
Each mention of a pair of entities is represented with a set of mention-based features $x$ and question-based features $x_t$.
For each predicate we have a latent variable $z$, that represents a relation (or none) expressed in the mention and depends on the features and a set of weights $w_x$, $w_t$ for mention-based and question-based features.
In this paper z variables are estimated using L2-regularized multinomial logistic regression from Stanford CoreNLP library.
Negative examples for training are sampled from entity pairs without existing Freebase relation (or with some relation other than relations of interest).
Finally, to predict a set of possible relations $\mathbf{y}$ between the pair of entities we take logical OR of individual mention variables $\mathbf{z}$, \ie $y_p = \lor_{i=1}^M [z_i = p, p \in P]$, where M is the number of mentions of this pair of entities.

\begin{figure}[ht]
\centering
\begin{tikzpicture}
\tikzstyle{main}=[circle, minimum size = 8mm, thick, draw =black!80, node distance = 14mm]
\tikzstyle{mainnob}=[circle, minimum size = 8mm, thick, draw =white!100, node distance = 14mm]
\tikzstyle{connect}=[-latex, thick]
\tikzstyle{box}=[rectangle, draw=black!100]
\node[main, fill = white!100] (y) [label=center:$y$] { };
\node[rectangle, inner sep=-1mm, fit=(y),label=below right:$P$] {};
\node[rectangle, inner sep=4mm, fit=(y),draw=black!100] {};
\node[main, fill = white!100] (z) [below=of y,label=center:$z$] { };
\node[rectangle, inner sep=-1mm, fit=(z),label=below right:$P$] {};
\node[rectangle, inner sep=4mm, fit=(z),draw=black!100] {};
\node[main, fill = black!10] (x) [below=of z,label=center:$\mathbf{x}$] { }; 
\node[main, fill = black!10] (t) [right=of z,label=center:$\mathbf{x_t}$] { };
\node[mainnob, fill = white!100] (wt) [right=of y,label=center:$\mathbf{w_t}$] { };
\node[mainnob, fill = white!100] (wx) [left=of y,label=center:$\mathbf{w_x}$] { };
\node[rectangle, inner sep=-1mm, fit=(z)(x)(t),label=below right:$|Q|$, yshift=-1mm] {};
\node[rectangle, inner sep=6.5mm, fit=(z)(x)(t),draw=black!100] {};

\node[rectangle, inner sep=-1mm, fit=(z)(x),label=below right:$M$,yshift=-12mm] {};
\node[rectangle, inner sep=5.0mm, fit=(z)(x),draw=black!100] {};

\node[rectangle, inner sep=-1mm, fit=(x)(z)(y),label=below right:$N$,yshift=-30mm,xshift=4.5mm] {};
\node[rectangle, inner sep=9mm, fit=(x)(z)(y),draw=black!100, yshift=-3mm] {};

\path (wx) edge [connect] (z)
(x) edge [connect] (z)
(z) edge [connect] (y)
(wt) edge [connect] (z)
(t) edge [connect] (z);
\end{tikzpicture}
\vspace{-15mm}
\caption{QnA-based relation extraction model plate diagram.\\
$N$ - number of different entity pairs, $M$ - number of mentions of an entity pair, $|Q|$ - number of questions where an entity pair is mentioned, $\mathbf{x}$ and $\mathbf{x_t}$ - mention-based and question-based features, $\mathbf{w}$ and $\mathbf{w_t}$ - corresponding feature weights, latent variables $z$ - predicates expressed in an entity pair mention, latent variables $y$ - relations between entity pair}
\label{fig:graphmodel}
\end{figure}

\subsection{Sentence-based baseline model}

Existing sentence-based relation extraction models can be applied to individual sentences of a QnA pair and will work well for complete statements, \eg ``Who did Brad Pitt marry? Brad Pitt and Angelina Jolie married at secret ceremony''.
We implemented the Mintz++ model described in \cite{Surdeanu:2012:MML:2390948.2391003}, which is an example of model in Figure \ref{fig:graphmodel}, where a set of question-based features is empty.
This model was shown to be superior to the original model in \cite{mintz_acl2009}, is easier to train than some other state of the art distant supervision models and produces comparable results.

\subsection{Sentence-based model with question features}

\begin{table*}[tbh]
\centering
\caption{Examples of features used for relation extraction for ``\emph{When was Mariah Carey born? Mariah Carey was born 27 March 1970}''}
\vspace{-2mm}
\label{table:features}
\begin{tabular}{|p{4.5cm}|p{11cm}|}
\hline
\multicolumn{2}{|c|}{Sentence-based model}\\
\hline
Dependency path & [PERSON]$\rightarrow$nsubjpass(born)tmod$\leftarrow$[DATE]\\
Surface pattern & [PERSON] be/VBD born/VBN [DATE]\\
\hline
\hline
\multicolumn{2}{|c|}{Question features for sentence-based model}\\
\hline
Question template & when [PERSON] born\\
Question deppath & (when)$\rightarrow$advmod(born)\\
Question word + deptree root & when+born\\
\hline
\hline
\multicolumn{2}{|c|}{QnA-based model}\\
\hline
Question template & Q: when [PERSON] born A:[DATE]\\
Question + answer deppath & Q:(when)$\rightarrow$advmod(born)nsubj$\leftarrow$[PERSON] A: (born)tmod$\leftarrow$[DATE]\\
Question word + deptree root & Q: when+born A:[DATE]\\
\hline
\end{tabular}
\end{table*}

In many cases an answer statement is hard to interpret correctly without knowing the corresponding question.
To give the baseline model some knowledge about the question, we include question-based features, which can help to account for the question topic and improve predictions in some ambiguous situations.
Question features are (Table \ref{table:features}): the category of a question in Yahoo! Answers, question template, concatenation of a question word with the root of a dependency tree and a dependency path from a question word to a verb if present.

\subsection{QnA-based model}
The QnA model for relation extraction is inspired by the observation, that often an answer sentence do not mention one of the entities at all, \eg ``\emph{When was Isaac Newton born? December 25, 1642 Woolsthorpe, England}''.
To tackle this situation we developed a discourse model for QnA pairs.
The assumption made is that an entity mentioned in a question is related to entities in the corresponding answer and the context of both mentions can be used to infer the relation predicate.
The model represent each entity pair with a set of question sentence and answer sentence features and predicts a possible relation between them.
Here is the list of features used (examples are given in \ref{table:features}):
question category in Yahoo! Answers, conjunction of a question word and the dependency tree root, question template, dependency path from a question word to the entity in the question, dependency path from the answer sentence root to the answer entity and the answer dependency tree root.
We also include conjunctions between question- and answer-based features.

