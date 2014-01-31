#!/usr/bin/perl -w

# Compute MAP and mean P(10) over different topic sets plus
# area under the curve and number topics with
# no relevant for those topic sets for a run
# using the TREC 2004 robust track topics.
#
# Input file is a table of scores of the form
# topicid avep p10
# for all topics in the run

use strict;

my %topic_sets = (
   "new" =>
   [651,  652,  653,  654,  655,  656,  657,  658,  659,  660,
    661,  662,  663,  664,  665,  666,  667,  668,  669,  670,
    671,        673,  674,  675,  676,  677,  678,  679,  680,
    681,  682,  683,  684,  685,  686,  687,  688,  689,  690,
    691,  692,  693,  694,  695,  696,  697,  698,  699,  700],

   "old" => 
   [301,  302,  303,  304,  305,  306,  307,  308,  309,  310,
    311,  312,  313,  314,  315,  316,  317,  318,  319,  320,
    321,  322,  323,  324,  325,  326,  327,  328,  329,  330,
    331,  332,  333,  334,  335,  336,  337,  338,  339,  340,
    341,  342,  343,  344,  345,  346,  347,  348,  349,  350,
    351,  352,  353,  354,  355,  356,  357,  358,  359,  360,
    361,  362,  363,  364,  365,  366,  367,  368,  369,  370,
    371,  372,  373,  374,  375,  376,  377,  378,  379,  380,
    381,  382,  383,  384,  385,  386,  387,  388,  389,  390,
    391,  392,  393,  394,  395,  396,  397,  398,  399,  400,
    401,  402,  403,  404,  405,  406,  407,  408,  409,  410,
    411,  412,  413,  414,  415,  416,  417,  418,  419,  420,
    421,  422,  423,  424,  425,  426,  427,  428,  429,  430,
    431,  432,  433,  434,  435,  436,  437,  438,  439,  440,
    441,  442,  443,  444,  445,  446,  447,  448,  449,  450,
    601,  602,  603,  604,  605,  606,  607,  608,  609,  610,
    611,  612,  613,  614,  615,  616,  617,  618,  619,  620,
    621,  622,  623,  624,  625,  626,  627,  628,  629,  630,
    631,  632,  633,  634,  635,  636,  637,  638,  639,  640,
    641,  642,  643,  644,  645,  646,  647,  648,  649,  650],

   "hard" =>
   [303,  322,  344,  353,  363,  378,  394,  408,  426,  439,
    307,  325,  345,  354,  367,  379,  397,  409,  427,  442,
    310,  330,  346,  355,  372,  383,  399,  414,  433,  443,
    314,  336,  347,  356,  374,  389,  401,  416,  435,  445,
    320,  341,  350,  362,  375,  393,  404,  419,  436,  448],
);

my (%is_hard, %is_new, %is_old);
my $scorefile;
my $line;
my ($tid,$ap,$p);
my (%avep,%p10);
my ($set, $quarter, $numtopics);
my ($area, $map, $prec, $zeros);

foreach $tid (@{$topic_sets{"old"}}) { 
    $is_hard{$tid} = 0;
    $is_new{$tid} = 0;
    $is_old{$tid} = 1;
}
foreach $tid (@{$topic_sets{"new"}}) {
    $is_hard{$tid} = 0;
    $is_new{$tid} = 1;
    $is_old{$tid} = 0;
}
foreach $tid (@{$topic_sets{"hard"}}) {
    $is_hard{$tid} = 1;
}


$#ARGV == 0 || die "Usage: robust_eval.pl scorefile\n";
$scorefile = $ARGV[0];
if ( (!-e $scorefile) || (! open SCORES, "<$scorefile") ) {
    die "Can't find/open scorefile '$scorefile': $!\n";
}
while ($line = <SCORES>) {
    chomp $line;
    next if ($line =~ /^\s*$/);

    ($tid,$ap,$p) =  split " ", $line;
    $avep{$tid} = $ap;
    $p10{$tid} = $p;
}
close SCORES || die "Can't close scorefile: $!\n";
    
    
foreach $set ("old", "new", "hard") {
    $numtopics = $#{$topic_sets{$set}} + 1; 
    $quarter = int $numtopics / 4;
    print "Summary measures over $numtopics $set topics\n";

    $map = 0; $prec = 0; $zeros = 0;
    foreach $tid (@{$topic_sets{$set}}) {
	$map += $avep{$tid};
  	$prec += $p10{$tid};
	if ($p10{$tid} == 0.0) {
	    $zeros++;
	}
    }
    $map /= $numtopics;
    $prec /= $numtopics;
    $area = &maparea($set);
    printf  "MAP: %.4f\n", $map;
    printf  "P(10): %.4f\n", $prec;
    print  "Number of topics with no relevant in top 10: ";
    printf "%3d/%-3d = %.1f%%\n",
		$zeros,$numtopics,($zeros/$numtopics)*100;
    print  "Area underneath MAP(X) vs. X curve for worst $quarter topics: ";
    printf "%.4f\n", $area;
    print "\n";
}

$numtopics = $#{$topic_sets{"old"}} + $#{$topic_sets{"new"}} + 2;
$quarter = int $numtopics / 4;
print "Summary measures over all topics\n";
$map = 0; $prec = 0; $zeros = 0;
foreach $tid (@{$topic_sets{"old"}}, @{$topic_sets{"new"}}) {
    $map += $avep{$tid};
    $prec += $p10{$tid};
    if ($p10{$tid} == 0.0) {
        $zeros++;
    }
}
$map /= $numtopics;
$prec /= $numtopics;
$area = &maparea("all");
printf  "MAP: %.4f\n", $map;
printf  "P(10): %.4f\n", $prec;
print  "Number of topics with no relevant in top 10: ";
printf "%3d/%-3d = %.1f%%\n",
	$zeros,$numtopics,($zeros/$numtopics)*100;
print  "Area underneath MAP(X) vs. X curve for worst $quarter topics: ";
printf "%.4f\n", $area;



# compute area under the MAP(X) vs. X curve for the worst
# quarter topics
sub maparea {
    my ($set) = @_;

    my ($map, $area, $areasum, $mapsum);
    my @aveprec;
    my $quarter;
    my ($t,$i);

    undef @aveprec;
    if ($set ne "all") {
	$i = 0;
	foreach $t (@{$topic_sets{$set}}) { 
	    $aveprec[$i++] = $avep{$t};
	}
	$quarter = int $i/4;
    }
    else {
	$i = 0;
	foreach $t (@{$topic_sets{"old"}}, @{$topic_sets{"new"}} ) { 
	    $aveprec[$i++] = $avep{$t};
	}
	$quarter = int $i/4;
    }

    @aveprec = sort bynum @aveprec;
    $areasum = 0;
    $mapsum = 0;
    for ($i=0; $i<$quarter; $i++) {
	$mapsum += $aveprec[$i];
	$map = $mapsum/($i+1);
    	$areasum += $map;
    }
    $area = $areasum / $quarter; 

    return $area;
}


sub bynum {
    $a <=> $b;
}
