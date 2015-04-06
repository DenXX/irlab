Relation extraction from natural language text has been an active area of research for many years, and a number of supervised \cite{snow2004learning}, semi-supervised \cite{Agichtein:2000:SER:336597.336644} and unsupervised \cite{Fader:2011:IRO:2145432.2145596} methods have been proposed.
These techniques analyze individual sentences and can extract facts stated in them using syntactic patterns, sentence similarity, \etc.
This work focus on one particular type of text data, \ie QnA pairs, and the proposed algorithm is designed to extract relations between entities mentioned in question and answer sentences.

Community question-answering data has been a subject of active research during the last decade.
The most closely related direction to our work is question answering, \eg \cite{Bian:2008:FRF:1367497.1367561} and \cite{Shtok:2012:LPA:2187836.2187939}.
We should also mention question answering in general, which is an area with a long history of research.
Numerous different approaches have been proposed over the decades and a good overview can be found in \newcite{Kolomiyets:2011:SQA:2046840.2047162}.
One particular way to answer questions is to utilize structured KBs and perform semantic parsing of questions to transform natural language questions into KB queries.
\newcite{berant2013semantic} proposed a semantic parsing model that can be trained from QnA pairs, which are much easier to obtain than correct KB queries used previously.
However, unlike our approach, which takes noisy answer text provided by a CQA website user, the work of \newcite{berant2013semantic} uses manually created answers in a form of single or lists of KB entities.
Later \newcite{yao2014information} presented an information extraction inspired approach, that predicts which of the entities related to an entity in the question could be the answer to the question.
The key difference of this work from question answering is that our relation extraction model doesn't target question understanding problem and doesn't necessarily extract the answer to the question, but rather some knowledge it can infer from a QnA pair.
Many questions on CQA websites are not factoid, and there are many advice and opinion questions, which simply cannot be answered with a KB entity or a list of entities.
However, it is still possible to learn some information from them (\eg from ``\emph{What's your favorite Stephen King book? The Dark Half is a pretty incredible book}'' we can learn that the Dark Half is a book by Stephen King).
In addition, answers provided by CQA users often contain extra information, which can also be useful (\eg from ``\emph{Where was Babe Ruth born? He was born in Baltimore, Maryland on February 6th, 1895}'' we can learn not only place of birth, but also date of birth of Babe Ruth).

% We can include an example about "Who is the president" and nationality predicate.