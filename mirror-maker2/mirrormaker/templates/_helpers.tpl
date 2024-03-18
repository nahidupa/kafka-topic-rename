{{/*
Expand the name of the chart.
*/}}
{{- define "mirrormaker-ks.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "mirrormaker-ks.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "mirrormaker-ks.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "mirrormaker-ks.labels" -}}
helm.sh/chart: {{ include "mirrormaker-ks.chart" . }}
{{ include "mirrormaker-ks.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "mirrormaker-ks.selectorLabels" -}}
app.kubernetes.io/name: {{ include "mirrormaker-ks.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "mirrormaker-ks.serviceAccountName" -}}
mirrormaker-ks-{{ .Release.Name }}
{{- end }}


{{/*
Custom Helm Template Function to Generate an Alias
Usage: {{ generateAlias $index $sourcePrefix $topic.source }}
*/}}
{{- define "generateAlias" -}}
{{- $index := index . 0 }}
{{- $sourcePrefix := index . 1 }}
{{- $source := index . 2 }}
{{- $alias := printf "%d-%s-%s" $index $sourcePrefix $source | replace "." "-" | replace "_" "-" | lower | trunc 40 }}
{{- $alias }}
{{- end -}}


