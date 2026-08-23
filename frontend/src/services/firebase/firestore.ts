import {
  collection,
  doc,
  getDoc,
  getDocs,
  setDoc,
  updateDoc,
  query,
  where,
  orderBy,
  limit,
  serverTimestamp,
} from 'firebase/firestore';
import type {
  CollectionReference,
  DocumentReference,
  DocumentData,
  QueryConstraint,
  SetOptions,
  UpdateData,
} from 'firebase/firestore';
import { db } from '../../config/firebase';

/**
 * Get typed collection reference
 */
export const getCollectionRef = <T = DocumentData>(collectionName: string): CollectionReference<T> => {
  return collection(db, collectionName) as CollectionReference<T>;
};

/**
 * Get typed document reference
 */
export const getDocRef = <T = DocumentData>(collectionName: string, docId: string): DocumentReference<T> => {
  return doc(db, collectionName, docId) as DocumentReference<T>;
};

/**
 * Read single document by collection name and document ID
 */
export const fetchDocument = async <T = DocumentData>(
  collectionName: string,
  docId: string
): Promise<{ id: string; data: T } | null> => {
  const docRef = getDocRef<T>(collectionName, docId);
  const snapshot = await getDoc(docRef);
  if (!snapshot.exists()) {
    return null;
  }
  return { id: snapshot.id, data: snapshot.data() };
};

/**
 * Read all documents in a collection with optional query constraints
 */
export const fetchCollection = async <T = DocumentData>(
  collectionName: string,
  ...queryConstraints: QueryConstraint[]
): Promise<Array<{ id: string; data: T }>> => {
  const colRef = getCollectionRef<T>(collectionName);
  const q = queryConstraints.length > 0 ? query(colRef, ...queryConstraints) : colRef;
  const snapshot = await getDocs(q);
  return snapshot.docs.map((docSnap) => ({
    id: docSnap.id,
    data: docSnap.data(),
  }));
};

/**
 * Create or overwrite a document
 */
export const saveDocument = async <T extends DocumentData>(
  collectionName: string,
  docId: string,
  data: T,
  options?: SetOptions
): Promise<void> => {
  const docRef = getDocRef<T>(collectionName, docId);
  if (options) {
    await setDoc(docRef, data, options);
  } else {
    await setDoc(docRef, data);
  }
};

/**
 * Update specific fields in an existing document
 */
export const updateDocumentFields = async <T extends DocumentData>(
  collectionName: string,
  docId: string,
  data: UpdateData<T>
): Promise<void> => {
  const docRef = getDocRef<T>(collectionName, docId);
  await updateDoc(docRef, data);
};

// Export query utilities for convenient query composition
export { collection, doc, getDoc, getDocs, setDoc, updateDoc, query, where, orderBy, limit, serverTimestamp };
