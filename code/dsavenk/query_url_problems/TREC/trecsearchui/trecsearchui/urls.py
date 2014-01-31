from django.conf.urls import patterns, include, url

# Uncomment the next two lines to enable the admin:
# from django.contrib import admin
# admin.autodiscover()

urlpatterns = patterns('',
	url(r'^$', 'main.views.main', name='main'),
	url(r'^index.html$', 'main.views.main', name='main'),
	url(r'^doc/(?P<topic>[0-9]+)/(?P<doc_id>[0-9]+)/$', 'main.views.show_doc_view', name='show_doc'),
    # Examples:
    # url(r'^$', 'trecsearchui.views.home', name='home'),
    # url(r'^trecsearchui/', include('trecsearchui.foo.urls')),

    # Uncomment the admin/doc line below to enable admin documentation:
    # url(r'^admin/doc/', include('django.contrib.admindocs.urls')),

    # Uncomment the next line to enable the admin:
    # url(r'^admin/', include(admin.site.urls)),
)
