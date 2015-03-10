This work targets the problem of relation extraction from QnA data, which is a collection of $(q, a)$ pairs, where $q$ is a question text (can contain multiple sentences) and $a$ is the corresponding answer text (can also contain multiple sentences).

By relation instance $r$ we mean an ordered binary relation between $subject$ and $object$ entities, which is commonly represented as $[subject, predicate, object]$ triple.
For example, the fact that Brad Pitt married Angelina Jolie can be represented as [Brad Pitt, married\_to, Angelina Jolie].
In this work we use Freebase, an open schema-based KB, where all entities and predicates come from the fixed alphabets $E$ and $P$ correspondingly.
We follow the distant supervision approach for relation extraction \cite{mintz_acl2009}.
The relation extraction problem can be formulated as a multiple instance multi-label classification problem \cite{Surdeanu:2012:MML:2390948.2391003}.
Let $e_1$ and $e_2$ be entities that are mentioned together in a text (\eg in a sentence, or $e_1$ in a question and $e_2$ in the corresponding answer).
The same pair of entities can be mentioned multiple times within the corpus, and for each mentions $i=1,...,n$ the goal is to predict the predicate expressed in the mention ($z_i \in P$) or to say that none applies ($z_i = \emptyset$).
In this work we represent each mention with a set of features $x_i$ and solve the problem as a multiclass classification problem:
$\hat{z_i} = \argmax{z_i \in P \cup \emptyset} p(z_i|x_i, e_1, e_2)$.

Each mention prediction may be noisy, but it gives us some information about the entities, and we can combine mention predictions $z_1, ..., z_n$ to infer a set of relations $\mathbf{y}=\{y_i \in P\}$ between the entities $e_1$ and $e_2$.
