# NOTE: All Python images seem to have a vulnerability (CVE-2025-69720)
# as of making this.
#
# Update if a fix is released.
#
FROM python:3.14.3-slim

WORKDIR /validator

COPY ngafid-validate/requirements.txt /validator/requirements.txt
RUN pip install --no-cache-dir -r /validator/requirements.txt

COPY ngafid-validate/validator.py /validator/validator.py
COPY ngafid-validate/validation-scripts /validator/validation-scripts

ENTRYPOINT ["python", "/validator/validator.py"]
