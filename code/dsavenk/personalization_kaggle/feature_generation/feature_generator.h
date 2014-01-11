#pragma once

#include <unordered_set>
#include <iostream>
#include <ostream>
#include <sstream>
#include <string>

#include "session.h"
#include "stats_collector.h"

struct Features {
    double qclicks1;
    double qclicks2;
    double qclicks3;
    double qclicks4;
    double qclicks5;
    double qclicks6;
    double qclicks7;
    double qclicks8;
    double qclicks9;
    double qclicks10;
    double qentropy;
    double uclicks1;
    double uclicks2;
    double uclicks3;
    double uclicks4;
    double uclicks5;
    double uclicks6;
    double uclicks7;
    double uclicks8;
    double uclicks9;
    double uclicks10;
    double uentropy;

    int u_shows;
    int u_clicks;
    double u_ctr;
    double u_ctr2;
    double u_satctr;
    double u_ssatctr;
    double u_avedwell;
    int qu_shows;
    int qu_clicks;
    double qu_ctr;
    double qu_ctr2;
    double qu_satctr;
    double qu_ssatctr;
    double qu_avedwell;
    int d_shows;
    int d_clicks;
    double d_ctr;
    double d_ctr2;
    double d_satctr;
    double d_ssatctr;
    double d_avedwell;
    int qd_shows;
    int qd_clicks;
    double qd_ctr;
    double qd_ctr2;
    double qd_satctr;
    double qd_ssatctr;
    double qd_avedwell;
    int u_avepos;
    int qu_avepos;
    int d_avepos;
    int qd_avepos;
    int u_lastinq_clicks;
    double u_lastinq_ctr;
    int u_lastinsession_clicks;
    double u_lastinsession_ctr;
    int qu_lastinq_clicks;
    double qu_lastinq_ctr;
    int qu_lastinsession_clicks;
    double qu_lastinsession_ctr;
    int d_lastinq_clicks;
    double d_lastinq_ctr;
    int d_lastinsession_clicks;
    double d_lastinsession_ctr;
    int qd_lastinq_clicks;
    double qd_lastinq_ctr;
    int qd_lastinsession_clicks;
    double qd_lastinsession_ctr;

    int q_terms;
    int td_shows;
    int td_clicks;
    double td_ctr;
    double td_ctr2;
    double td_avedwell;
    int tu_shows;
    int tu_clicks;
    double tu_ctr;

    double prevtu_ctr;
    double prevtd_ctr;
    double prevtu_ctr_user;
    double prevtd_ctr_user;

    double tu_ctr2;
    double tu_avedwell;
    int td_shows_user;
    int td_clicks_user;
    double td_ctr_user;
    double td_avedwell_user;
    int tu_shows_user;
    int tu_clicks_user;
    double tu_ctr_user;
    double tu_ctr2_user;
    double td_ctr2_user;
    double tu_avedwell_user;

    double max_u_prevq_ctr;
    double max_u_prevu_ctr;
    double max_d_prevq_ctr;
    double max_d_prevd_ctr;

    int have_user_history;
    int u_shows_user;
    int u_clicks_user;
    double u_ctr_user;
    double u_ctr2_user;
    double u_satctr_user;
    double u_ssatctr_user;
    double u_avedwell_user;
    int qu_shows_user;
    int qu_clicks_user;
    double qu_ctr_user;
    double qu_ctr2_user;
    double qu_satctr_user;
    double qu_ssatctr_user;
    double qu_avedwell_user;
    int d_shows_user;
    int d_clicks_user;
    double d_ctr_user;
    double d_ctr2_user;
    double d_satctr_user;
    double d_ssatctr_user;
    double d_avedwell_user;
    int qd_shows_user;
    int qd_clicks_user;
    double qd_ctr_user;
    double qd_ctr2_user;
    double qd_satctr_user;
    double qd_ssatctr_user;
    double qd_avedwell_user;
    int u_avepos_user;
    int qu_avepos_user;
    int d_avepos_user;
    int qd_avepos_user;
    int u_lastinq_clicks_user;
    double u_lastinq_ctr_user;
    int u_lastinsession_clicks_user;
    double u_lastinsession_ctr_user;
    int qu_lastinq_clicks_user;
    double qu_lastinq_ctr_user;
    int qu_lastinsession_clicks_user;
    double qu_lastinsession_ctr_user;
    int d_lastinq_clicks_user;
    double d_lastinq_ctr_user;
    int d_lastinsession_clicks_user;
    double d_lastinsession_ctr_user;
    int qd_lastinq_clicks_user;
    double qd_lastinq_ctr_user;
    int qd_lastinsession_clicks_user;
    double qd_lastinsession_ctr_user;

    double max_u_prevq_ctr_user;
    double max_u_prevu_ctr_user;
    double max_d_prevq_ctr_user;
    double max_d_prevd_ctr_user;

    int query_asked;
    int url_shown;
    int url_clicked;
    int cur_total_dwell;
    int time_passed;
    int cur_queries;
    int cur_clicks;

    int cur_pos;

    static std::string getHeader() {
        std::stringstream str;
       str << "u_shows"<< ",";
       str << "u_clicks"<< ",";
       str << "u_ctr"<< ",";
       str << "u_ctr2"<< ",";
       str << "u_satctr"<< ",";
       str << "u_ssatctr"<< ",";
       str << "u_avedwell"<< ",";
       str << "qu_shows"<< ",";
       str << "qu_clicks"<< ",";
       str << "qu_ctr"<< ",";
       str << "qu_ctr2"<< ",";
       str << "qu_satctr"<< ",";
       str << "qu_ssatctr"<< ",";
       str << "qu_avedwell"<< ",";
       
       str << "q_terms" << ",";
       str << "tu_shows" << ",";
       str << "tu_clicks" << ",";
       str << "tu_ctr" << ",";
       str << "tu_ctr2" << ",";
       str << "tu_avedwell" << ",";

      str << "prevtu_ctr" << ",";
      str << "prevtd_ctr" << ",";
      str << "prevtu_ctr_user" << ",";
      str << "prevtd_ctr_user" << ",";

       str << "d_shows"<< ",";
       str << "d_clicks"<< ",";
       str << "d_ctr"<< ",";
       str << "d_ctr2"<< ",";
       str << "d_satctr"<< ",";
       str << "d_ssatctr"<< ",";
       str << "d_avedwell"<< ",";
       str << "qd_shows"<< ",";
       str << "qd_clicks"<< ",";
       str << "qd_ctr"<< ",";
       str << "qd_ctr2"<< ",";
       str << "qd_satctr"<< ",";
       str << "qd_ssatctr"<< ",";
       str << "qd_avedwell"<< ",";

       str << "td_shows" << ",";
       str << "td_clicks" << ",";
       str << "td_ctr" << ",";
       str << "td_ctr2" << ",";
       str << "td_avedwell" << ",";

       str << "u_avepos"<< ",";
       str << "qu_avepos"<< ",";
       str << "d_avepos"<< ",";
       str << "qd_avepos"<< ",";
       str << "u_lastinq_clicks"<< ",";
       str << "u_lastinq_ctr"<< ",";
       str << "u_lastinsession_clicks"<< ",";
       str << "u_lastinsession_ctr"<< ",";
       str << "qu_lastinq_clicks"<< ",";
       str << "qu_lastinq_ctr"<< ",";
       str << "qu_lastinsession_clicks"<< ",";
       str << "qu_lastinsession_ctr"<< ",";
       str << "d_lastinq_clicks"<< ",";
       str << "d_lastinq_ctr"<< ",";
       str << "d_lastinsession_clicks"<< ",";
       str << "d_lastinsession_ctr"<< ",";
       str << "qd_lastinq_clicks"<< ",";
       str << "qd_lastinq_ctr"<< ",";
       str << "qd_lastinsession_clicks"<< ",";
       str << "qd_lastinsession_ctr"<< ",";

       str << "max_u_prevq_ctr"<< ",";
       str << "max_u_prevu_ctr"<< ",";
       str << "max_d_prevq_ctr"<< ",";
       str << "max_d_prevd_ctr"<< ",";

       str << "have_user_history"<< ",";
       str << "u_shows_user"<< ",";
       str << "u_clicks_user"<< ",";
       str << "u_ctr_user"<< ",";
       str << "u_ctr2_user"<< ",";
       str << "u_satctr_user"<< ",";
       str << "u_ssatctr_user"<< ",";
       str << "u_avedwell_user"<< ",";
       str << "qu_shows_user"<< ",";
       str << "qu_clicks_user"<< ",";
       str << "qu_ctr_user"<< ",";
       str << "qu_ctr2_user"<< ",";
       str << "qu_satctr_user"<< ",";
       str << "qu_ssatctr_user"<< ",";
       str << "qu_avedwell_user"<< ",";
       str << "d_shows_user"<< ",";
       str << "d_clicks_user"<< ",";
       str << "d_ctr_user"<< ",";
       str << "d_ctr2_user"<< ",";
       str << "d_satctr_user"<< ",";
       str << "d_ssatctr_user"<< ",";
       str << "d_avedwell_user"<< ",";
       str << "qd_shows_user"<< ",";
       str << "qd_clicks_user"<< ",";
       str << "qd_ctr_user"<< ",";
       str << "qd_ctr2_user"<< ",";
       str << "qd_satctr_user"<< ",";
       str << "qd_ssatctr_user"<< ",";
       str << "qd_avedwell_user"<< ",";
       str << "u_avepos_user"<< ",";
       str << "qu_avepos_user"<< ",";
       str << "d_avepos_user"<< ",";
       str << "qd_avepos_user"<< ",";
       str << "u_lastinq_clicks_user"<< ",";
       str << "u_lastinq_ctr_user"<< ",";
       str << "u_lastinsession_clicks_user"<< ",";
       str << "u_lastinsession_ctr_user"<< ",";
       str << "qu_lastinq_clicks_user"<< ",";
       str << "qu_lastinq_ctr_user"<< ",";
       str << "qu_lastinsession_clicks_user"<< ",";
       str << "qu_lastinsession_ctr_user"<< ",";
       str << "d_lastinq_clicks_user"<< ",";
       str << "d_lastinq_ctr_user"<< ",";
       str << "d_lastinsession_clicks_user"<< ",";
       str << "d_lastinsession_ctr_user"<< ",";
       str << "qd_lastinq_clicks_user"<< ",";
       str << "qd_lastinq_ctr_user"<< ",";
       str << "qd_lastinsession_clicks_user"<< ",";
       str << "qd_lastinsession_ctr_user"<< ",";

       str << "max_u_prevq_ctr_user"<< ",";
       str << "max_u_prevu_ctr_user"<< ",";
       str << "max_d_prevq_ctr_user"<< ",";
       str << "max_d_prevd_ctr_user"<< ",";

       str << "query_asked"<< ",";
       str << "url_shown"<< ",";
       str << "url_clicked"<< ",";
       str << "cur_total_dwell"<< ",";
       str << "time_passed"<< ",";
       str << "cur_queries"<< ",";
       str << "cur_clicks"<< ",";
       str << "cur_pos" << ",";

        str << "tu_shows_user" << ",";
        str << "tu_clicks_user" << ",";
        str << "tu_ctr_user" << ",";
        str << "tu_ctr2_user" << ",";
        str << "tu_avedwell_user" << ",";
        str << "td_shows_user" << ",";
        str << "td_clicks_user" << ",";
        str << "td_ctr_user" << ",";
        str << "td_ctr2_user" << ",";
        str << "td_avedwell_user" << ",";

        str << "qclicks1" << ",";
        str << "qclicks2" << ",";
        str << "qclicks3" << ",";
        str << "qclicks4" << ",";
        str << "qclicks5" << ",";
        str << "qclicks6" << ",";
        str << "qclicks7" << ",";
        str << "qclicks8" << ",";
        str << "qclicks9" << ",";
        str << "qclicks10" << ",";
        str << "qentropy" << ",";
        str << "uclicks1" << ",";
        str << "uclicks2" << ",";
        str << "uclicks3" << ",";
        str << "uclicks4" << ",";
        str << "uclicks5" << ",";
        str << "uclicks6" << ",";
        str << "uclicks7" << ",";
        str << "uclicks8" << ",";
        str << "uclicks9" << ",";
        str << "uclicks10" << ",";
        str << "uentropy";

       return str.str();
    }

    std::string toString() {
        std::stringstream str;
       str << u_shows<< ",";
       str << u_clicks<< ",";
       str << u_ctr<< ",";
       str << u_ctr2<< ",";
       str << u_satctr<< ",";
       str << u_ssatctr<< ",";
       str << u_avedwell<< ",";
       str << qu_shows<< ",";
       str << qu_clicks<< ",";
       str << qu_ctr<< ",";
       str << qu_ctr2<< ",";
       str << qu_satctr<< ",";
       str << qu_ssatctr<< ",";
       str << qu_avedwell<< ",";
       
       str << q_terms << ",";
       str << tu_shows << ",";
       str << tu_clicks << ",";
       str << tu_ctr << ",";
       str << tu_ctr2 << ",";
       str << tu_avedwell << ",";

      str << prevtu_ctr << ",";
      str << prevtd_ctr << ",";
      str << prevtu_ctr_user << ",";
      str << prevtd_ctr_user << ",";

       str << d_shows<< ",";
       str << d_clicks<< ",";
       str << d_ctr<< ",";
       str << d_ctr2<< ",";
       str << d_satctr<< ",";
       str << d_ssatctr<< ",";
       str << d_avedwell<< ",";
       str << qd_shows<< ",";
       str << qd_clicks<< ",";
       str << qd_ctr<< ",";
       str << qd_ctr2<< ",";
       str << qd_satctr<< ",";
       str << qd_ssatctr<< ",";
       str << qd_avedwell<< ",";

       str << td_shows << ",";
       str << td_clicks << ",";
       str << td_ctr << ",";
       str << td_ctr2 << ",";
       str << td_avedwell << ",";

       str << u_avepos<< ",";
       str << qu_avepos<< ",";
       str << d_avepos<< ",";
       str << qd_avepos<< ",";
       str << u_lastinq_clicks<< ",";
       str << u_lastinq_ctr<< ",";
       str << u_lastinsession_clicks<< ",";
       str << u_lastinsession_ctr<< ",";
       str << qu_lastinq_clicks<< ",";
       str << qu_lastinq_ctr<< ",";
       str << qu_lastinsession_clicks<< ",";
       str << qu_lastinsession_ctr<< ",";
       str << d_lastinq_clicks<< ",";
       str << d_lastinq_ctr<< ",";
       str << d_lastinsession_clicks<< ",";
       str << d_lastinsession_ctr<< ",";
       str << qd_lastinq_clicks<< ",";
       str << qd_lastinq_ctr<< ",";
       str << qd_lastinsession_clicks<< ",";
       str << qd_lastinsession_ctr<< ",";

       str << max_u_prevq_ctr<< ",";
       str << max_u_prevu_ctr<< ",";
       str << max_d_prevq_ctr<< ",";
       str << max_d_prevd_ctr<< ",";

       str << have_user_history<< ",";
       str << u_shows_user<< ",";
       str << u_clicks_user<< ",";
       str << u_ctr_user<< ",";
       str << u_ctr2_user<< ",";
       str << u_satctr_user<< ",";
       str << u_ssatctr_user<< ",";
       str << u_avedwell_user<< ",";
       str << qu_shows_user<< ",";
       str << qu_clicks_user<< ",";
       str << qu_ctr_user<< ",";
       str << qu_ctr2_user<< ",";
       str << qu_satctr_user<< ",";
       str << qu_ssatctr_user<< ",";
       str << qu_avedwell_user<< ",";
       str << d_shows_user<< ",";
       str << d_clicks_user<< ",";
       str << d_ctr_user<< ",";
       str << d_ctr2_user<< ",";
       str << d_satctr_user<< ",";
       str << d_ssatctr_user<< ",";
       str << d_avedwell_user<< ",";
       str << qd_shows_user<< ",";
       str << qd_clicks_user<< ",";
       str << qd_ctr_user<< ",";
       str << qd_ctr2_user<< ",";
       str << qd_satctr_user<< ",";
       str << qd_ssatctr_user<< ",";
       str << qd_avedwell_user<< ",";
       str << u_avepos_user<< ",";
       str << qu_avepos_user<< ",";
       str << d_avepos_user<< ",";
       str << qd_avepos_user<< ",";
       str << u_lastinq_clicks_user<< ",";
       str << u_lastinq_ctr_user<< ",";
       str << u_lastinsession_clicks_user<< ",";
       str << u_lastinsession_ctr_user<< ",";
       str << qu_lastinq_clicks_user<< ",";
       str << qu_lastinq_ctr_user<< ",";
       str << qu_lastinsession_clicks_user<< ",";
       str << qu_lastinsession_ctr_user<< ",";
       str << d_lastinq_clicks_user<< ",";
       str << d_lastinq_ctr_user<< ",";
       str << d_lastinsession_clicks_user<< ",";
       str << d_lastinsession_ctr_user<< ",";
       str << qd_lastinq_clicks_user<< ",";
       str << qd_lastinq_ctr_user<< ",";
       str << qd_lastinsession_clicks_user<< ",";
       str << qd_lastinsession_ctr_user<< ",";

       str << max_u_prevq_ctr_user<< ",";
       str << max_u_prevu_ctr_user<< ",";
       str << max_d_prevq_ctr_user<< ",";
       str << max_d_prevd_ctr_user<< ",";

       str << query_asked<< ",";
       str << url_shown<< ",";
       str << url_clicked<< ",";
       str << cur_total_dwell<< ",";
       str << time_passed<< ",";
       str << cur_queries<< ",";
       str << cur_clicks<< ",";
       str << cur_pos << ",";

        str << tu_shows_user << ",";
        str << tu_clicks_user << ",";
        str << tu_ctr_user << ",";
        str << tu_ctr2_user << ",";
        str << tu_avedwell_user << ",";
        str << td_shows_user << ",";
        str << td_clicks_user << ",";
        str << td_ctr_user << ",";
        str << td_ctr2_user << ",";
        str << td_avedwell_user << ",";

        str << qclicks1 << ",";
        str << qclicks2 << ",";
        str << qclicks3 << ",";
        str << qclicks4 << ",";
        str << qclicks5 << ",";
        str << qclicks6 << ",";
        str << qclicks7 << ",";
        str << qclicks8 << ",";
        str << qclicks9 << ",";
        str << qclicks10 << ",";
        str << qentropy << ",";
        str << uclicks1 << ",";
        str << uclicks2 << ",";
        str << uclicks3 << ",";
        str << uclicks4 << ",";
        str << uclicks5 << ",";
        str << uclicks6 << ",";
        str << uclicks7 << ",";
        str << uclicks8 << ",";
        str << uclicks9 << ",";
        str << uclicks10 << ",";
        str << uentropy;
        return str.str();
    }

    Features();
};

class FeatureGenerator {
public:
    
    static void writeFeatureHeader(std::ostream& out);
    static void writeFeatures(std::ostream& out, Session& session, StatsCollector& stats);
};
