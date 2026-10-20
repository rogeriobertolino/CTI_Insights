FROM python:3.12-slim
WORKDIR /app
COPY python/requirements.txt /app/python/requirements.txt
COPY analytics-python/requirements.txt /app/analytics-python/requirements.txt
RUN pip install --no-cache-dir -r /app/analytics-python/requirements.txt
COPY python /app/python
COPY analytics-python /app/analytics-python
ENV PYTHONIOENCODING=utf-8 OPENBLAS_NUM_THREADS=1 OMP_NUM_THREADS=1 MPLCONFIGDIR=/tmp/matplotlib
ENTRYPOINT ["python", "/app/python/analyze.py"]
