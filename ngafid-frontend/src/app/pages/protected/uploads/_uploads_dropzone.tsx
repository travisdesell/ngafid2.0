// ngafid-frontend/src/app/pages/protected/uploads/_uploads_dropdzone.tsx
import ErrorModal from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_context";
import {
    Card,
    CardDescription,
    CardHeader,
    CardTitle,
} from "@/components/ui/card";
import { FileUp, Shredder } from "lucide-react";
import { useCallback, useMemo } from "react";
import { useDropzone } from "react-dropzone";

type Props = {
    onPickFiles: (files: FileList | null) => Promise<void>;
};

const SUPPORTED_FILE_TYPES = [
    "application/zip",
    "application/x-zip-compressed",
    "multipart/x-zip",
    "application/x-7z-compressed",
    "application/x-rar-compressed",
    "application/gzip",
    "application/x-tar",
];

export default function UploadsDropzone({ onPickFiles }: Props) {

    const { setModal } = useModal();

    const onDrop = useCallback(
        (acceptedFiles: File[]) => {
            acceptedFiles.forEach((file) => {

                // Unsupported file type, show error modal
                if (!SUPPORTED_FILE_TYPES.includes(file.type)) {

                    setModal(ErrorModal, {
                        title: "Unsupported File Type",
                        message: `The file type "${file.type}" is not supported.`,
                    });

                    return;

                }

                const dataTransferInst = new DataTransfer();
                acceptedFiles.forEach((f) => dataTransferInst.items.add(f));

                void onPickFiles(dataTransferInst.files);

            });

        },
        [onPickFiles, setModal]
    );

    const {
        getRootProps,
        getInputProps,
        isDragActive,
        isDragAccept,
        isDragReject,
    } = useDropzone({
        onDrop,
        accept: Object.fromEntries(SUPPORTED_FILE_TYPES.map((t) => [t, []])),
        multiple: true,
    });

    const cardClassName = useMemo(() => {

        const base = "relative cursor-pointer group transition-colors border-2 border-dashed";

        if (isDragReject)
            return `${base} border-destructive`;
        
        if (isDragAccept)
            return `${base} border-primary`;

        if (isDragActive)
            return `${base} border-primary/60`;

        return `${base} border-muted`;

    }, [isDragActive, isDragAccept, isDragReject]);

    return (
        <Card {...getRootProps()} className={cardClassName}>
            
            <input {...getInputProps()} />

            <CardHeader className="flex flex-col space-y-1.5">
                <CardTitle>Upload Files</CardTitle>
                <CardDescription>
                    {
                        (isDragReject)
                        ? "Detected unsupported files, please try again"
                        :
                        (isDragActive)
                        ? "Release to upload..."
                        : "Drag and drop files here, or click to select files."
                    }
                </CardDescription>
            </CardHeader>

            {
                (isDragReject)
                ?
                <Shredder
                    className={`
                        absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 opacity-50
                        transition-all duration-200 ease-in-out ${
                            (isDragActive)
                            ? "opacity-100 scale-125"
                            : "group-hover:opacity-100 group-hover:scale-125"
                        }`}
                    />
                :
                <FileUp
                    className={`
                        absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 opacity-50
                        transition-all duration-200 ease-in-out ${
                            (isDragActive)
                            ? "opacity-100 scale-125"
                            : "group-hover:opacity-100 group-hover:scale-125"
                        }`}
                    />
            }
        </Card>
    );

}
