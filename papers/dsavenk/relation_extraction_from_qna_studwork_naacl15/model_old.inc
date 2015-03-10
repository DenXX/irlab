\subsection{!!Copied from our IDF application}

% Copied from our IDF application.
We developed two algorithms to tackle the problem of extracting facts from Q\&A websites: 
(1) template-based extractor, 
(2) Max Entropy-based extractor. 
Before running the extractors, some pre-processing of the data is needed. 
We will first describe the pre-process steps and then the template-based and 
Max Entropy-based extractors.

\subsection{Pre-Processing}
(1) Extract Question \& Answer Text from the Web: 
The first step is to locate question texts and their corresponding answer texts on the web. 
In Q\&A websites, the answer texts may not immediately follow the question text. 
Sometimes they are in different parts of the page, sometimes there are several answer threads for the same question.
% Describe Q&A extractions here.

(2) Find Entities in Question and Answer Text: 
The second task is to find entities in the question and answer texts. 
% Describe NLP pipeline here

\subsection{Template-based Extractor}
The template-based extractor tries to utilize the question templates to extract facts from Q\&A. 
For example, if the question matches the template ``who [person] wife'', 
and the answer texts have another person’s name, then it is likely that there is a 
\url{/people/person/spouse_s} relation between the two entities.

The extractor has two components: (1) Matcher, (2) Aggregator. 
The matcher determines whether there is match between the question and the templates. 
The aggregator combines evidence from different matches and provide the final prediction 
whether there is a relation between two entities.

\subsubsection{Macher}
The matcher determine whether there is match between the question and the templates. 
Specifically, we have two kinds of templates: (a) surface-based templates, 
and (b) parser-based templates. 
The surface-based templates represents the question by plain words. 
If a word corresponds to an entity, then we replace the word by the entity’s type. 
In order to improve generalization, we also ignore stop words and punctuation in the templates. 
For example, the question ``Who does Brad Pitt marry to?'' will match to the template 
``who $\langle[PERSON]\rangle$ marry'' because ``does'' and ``to'' are stopwords, 
and ``Brad Pitt'' matches to the type $\langle[PERSON]\rangle$. 
If the question matches one of the template, 
we will search the answer texts to see if there is any entity that matches the expected type of the target relation. 
For example, if the target relation is \url{/people/person/spouse_s}, 
the system will search for the answer text to see if there is any entity having type $\langle[PERSON]\rangle$, 
which is the expected type of object of \url{/people/person/spouse_s}. 
If the system finds an entity with type $\langle[PERSON]\rangle$ in the answer texts, 
then there is a \emph{match} and the entity is potentially the wife of Brad Pitt.

Parser-based templates instead represent the question by parse tree, 
which represents the syntactic structure of the question. 
For example, the question ``Who is Brad Pitt married to?'' matches to the parse-based template 
``$\langle[PERSON]\rangle$ \url{/NOUN} \url{is/VERB} \url{married/VERB} \url{to/ADP} \url{/NOUN} $\langle[PERSON]\rangle$''. 
If the question matches one of the the template, 
the system will search the answer texts to seek any entity having the expected type of the target relation, 
as we do in the surface-based templates.

\subsubsection{Aggregator}
The aggregator combines all evidences from template matching and make a final decision 
whether there is a relation between two entities. 
After template matching, we will have several sources of evidences for a relation between two entities. 
For example, we may find several Q\&A pairs that match the templates, 
\eg, ``Q: Who is Brad Pitt’s wife? A: Jeniffer.'', ``Q: Who was Brad Pitt married to? A: Jennifer Aniston''. 
The more good-quality template matches we have, the more evident that Brad Pitt married to Jennifer. 
On the other hand, if there is a single match, the information may be a false alarm. 
To distinguish between true relation and false alarm, 
we combine several features together to produce the final judgment, 
such as the number of matches, number of unique template triggered, 
number of templates for the relation. 
All these features are combined into a machine learnt classifier to predict the probability of 
whether there is a relation between two entities.

\subsubsection{Training}
In order to find good templates for a relation, we use distant supervision \cite{mintz_acl2009}. 
Specifically, if the question and answer contains a pair of entities that have a known relation in Freebase, 
we create a candidate template from the question text for that relation. 
For instance, assuming that there is a Q\&A pair ``Q: Who was Brad Pitt married to? A: Jennifer Aniston.'' 
and there is an existing relation in Freebase (Brad Pitt, \url{/people/person/spouse_s}, Jennifer Aniston), 
then we can create a candidate surface template ``who [PERSON] married $\rightarrow$ [PERSON]'' from the Q\&A pair. 
After running this process for the entire dataset, 
we will find many candidate templates for a relation, 
of which has different number of occurrences in the dataset. 
We then determine whether the template is a good one based on 
(1) whether the number of times this templates appears in Q\&A pairs is bigger than a threshold, 
(2) the number of unique entity pairs that triggers this template is bigger than a threshold.

\subsection{Max Entropy Based Extractor}
The matcher in the template-based extractor makes a binary decision: match or non-match, 
to determine whether there is a potential relation between entities. 
The matcher also does not utilize valuable information from the answer texts (except the expected type) 
to help establish the relation between two entities. 
In order to make a \emph{soft} decision with a probability estimate and combine useful signals from multiple sources, 
we instead use a machine-learnt classifier to predict whether there is a relation between two entities instead of 
using the template matching approach.

Specifically, given two entities, one in the question text and another in the answer text, we compute a set of predefined features. 
These features include: 
(1) whether the question matches one of the surface template, and which template it matches, 
(2) whether the question matches one of the parse template, and which template it matches, 
(3) the entity’s types, 
(4) the parse path from the HEAD token in question to the question entity, 
(5) the parse path from HEAD token in answer to the answer entity, 
(6) whether the question entity appears in the answer text, 
(7) bag or words that appears in the left and right of the question and answer entities, 
(8) similarity measure between the question and answer text. 
All these features are fed into a Max Entropy Classifier to predict the probability whether there is a relation between two entities.

To train the classifier, we again use distant supervision \cite{mintz_acl2009}. 
Specifically, if there is a Q\&A with a pair of entities with a known relation in Freebase, 
we treat them as possible examples for that relation, but negative examples for other relations. 
Given sufficient training examples for a relation, we then optimize the parameters of the classifier for the Max Entropy objective function.