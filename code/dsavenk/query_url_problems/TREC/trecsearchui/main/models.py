from django.db import models

# Create your models here.
class Problem(models.Model):
	topic = models.PositiveIntegerField()
	query = models.CharField(max_length=255)
	docid = models.PositiveIntegerField()
	docno = models.CharField(max_length=32)
	problem = models.TextField()
	flag = models.PositiveIntegerField()